import tkinter as tk
from tkinter import filedialog, messagebox, scrolledtext
import xml.etree.ElementTree as ET
import hashlib
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import binascii
import csv
import sys

# --- Crypto Implementation ---

def pkcs12_kdf(password, salt, iter, n, id, hash_algo=hashlib.sha1):
    """
    PKCS#12 Key Derivation Function.
    Derives key (id=1) or IV (id=2) from password and salt.
    """
    u = hash_algo().digest_size
    v = 64 # block size (64 for MD5 and SHA1)

    def hash_func(data):
        return hash_algo(data).digest()

    D = bytes([id]) * v
    S = b''
    if salt:
        len_s = v * ((len(salt) + v - 1) // v)
        S = (salt * ((len_s + len(salt) - 1) // len(salt)))[:len_s]
    P = b''
    if password:
        len_p = v * ((len(password) + v - 1) // v)
        P = (password * ((len_p + len(password) - 1) // len(password)))[:len_p]
    I = S + P
    c = (n + u - 1) // u
    A = b''
    for i in range(1, c + 1):
        Ai = hash_func(D + I)
        for _ in range(1, iter):
            Ai = hash_func(Ai)
        A += Ai
        if i < c:
            B = (Ai * ((v + u - 1) // u))[:v]
            B_int = int.from_bytes(B, 'big')
            I_new = b''
            for j in range(0, len(I), v):
                chunk = I[j:j+v]
                chunk_int = int.from_bytes(chunk, 'big')
                new_chunk_int = (chunk_int + B_int + 1) % (2 ** (v * 8))
                # Ensure it's padded to v bytes
                I_new += new_chunk_int.to_bytes(v, 'big')
            I = I_new
    return A[:n]

def openssl_kdf(password, salt, iter, key_len, iv_len):
    """
    OpenSSL Legacy Key Derivation (EVP_BytesToKey) using MD5.
    """
    m = b''
    data = b''
    while len(data) < (key_len + iv_len):
        ctx = hashlib.md5()
        if m:
            ctx.update(m)
        ctx.update(password)
        if salt:
            ctx.update(salt)
        ms = ctx.digest()
        for _ in range(1, iter):
            ms = hashlib.md5(ms).digest()
        m = ms
        data += m
    key = data[:key_len]
    iv = data[key_len:key_len+iv_len]
    return key, iv

class OISafeDecryptor:
    def __init__(self, xml_file):
        try:
            self.tree = ET.parse(xml_file)
            self.root = self.tree.getroot()
        except Exception as e:
            raise ValueError(f"Invalid XML file: {e}")

        # Parse global salt
        salt_elem = self.root.find("Salt")
        if salt_elem is None or not salt_elem.text:
            raise ValueError("Salt not found in XML")
        self.salt_hex = salt_elem.text
        self.salt = binascii.unhexlify(self.salt_hex)

        # Parse encrypted master key
        mk_elem = self.root.find("MasterKey")
        if mk_elem is None or not mk_elem.text:
            raise ValueError("MasterKey not found in XML")
        self.encrypted_mk_hex = mk_elem.text

    def decrypt_master_key(self, password):
        """
        Decrypts the Master Key using the user password.
        Returns the decrypted Master Key (Hex String).
        """
        # Master Key is encrypted with Strong algorithm: PBEWithSHA1And256BitAES-CBC-BC
        # This uses PKCS#12 KDF with SHA1.

        # Standard PKCS#12 encoding: UTF-16BE + 2 null bytes
        try:
            pass_bytes = password.encode('utf-16be') + b'\x00\x00'
        except Exception:
            raise ValueError("Password encoding failed")

        key = pkcs12_kdf(pass_bytes, self.salt, 20, 32, 1)
        iv = pkcs12_kdf(pass_bytes, self.salt, 20, 16, 2)

        try:
            ciphertext = binascii.unhexlify(self.encrypted_mk_hex)
            cipher = AES.new(key, AES.MODE_CBC, iv)
            decrypted_mk_bytes = unpad(cipher.decrypt(ciphertext), AES.block_size)
            # The result is the Master Key (which is a hex string in the original code)
            return decrypted_mk_bytes.decode('utf-8')
        except Exception as e:
            raise ValueError("Incorrect Password or Corrupted Data")

    def decrypt_entries(self, master_key_hex):
        """
        Decrypts all entries using the decrypted Master Key.
        """
        # Entries are encrypted with Medium algorithm: PBEWithMD5And128BitAES-CBC-OpenSSL
        # Password for this is the Master Key (Hex String).

        # My tests showed success with ASCII encoding of the master key hex string.
        pass_bytes = master_key_hex.encode('ascii')

        key, iv = openssl_kdf(pass_bytes, self.salt, 20, 16, 16)

        entries = []

        for category in self.root.findall("Category"):
            cat_name = category.get("name")
            # Category names are encrypted
            try:
                cat_name_decrypted = self._decrypt_string(cat_name, key, iv)
            except:
                cat_name_decrypted = "[Encrypted]"

            for entry in category.findall("Entry"):
                data = {}
                data['Category'] = cat_name_decrypted

                # Fields to decrypt
                fields = ['Description', 'Website', 'Username', 'Password', 'Note']
                for f in fields:
                    val = entry.find(f)
                    if val is not None and val.text:
                        try:
                            data[f] = self._decrypt_string(val.text, key, iv)
                        except:
                            data[f] = "[Error]"
                    else:
                        data[f] = ""
                entries.append(data)
        return entries

    def _decrypt_string(self, hex_ciphertext, key, iv):
        if not hex_ciphertext:
            return ""
        try:
            ciphertext = binascii.unhexlify(hex_ciphertext)
            cipher = AES.new(key, AES.MODE_CBC, iv)
            plaintext = unpad(cipher.decrypt(ciphertext), AES.block_size)
            return plaintext.decode('utf-8')
        except Exception as e:
            raise ValueError(f"Decryption error: {e}")

# --- GUI Implementation ---

class DecryptionApp:
    def __init__(self, root):
        self.root = root
        self.root.title("OI Safe Decryptor")
        self.root.geometry("1000x600")

        # Variables
        self.file_path = tk.StringVar()
        self.password = tk.StringVar()
        self.decrypted_data = []

        # UI Layout
        self._create_widgets()

    def _create_widgets(self):
        # File Selection Frame
        frame_file = tk.LabelFrame(self.root, text="1. Select Backup File", padx=10, pady=10)
        frame_file.pack(fill="x", padx=10, pady=5)

        tk.Entry(frame_file, textvariable=self.file_path, width=50).pack(side=tk.LEFT, fill="x", expand=True, padx=5)
        tk.Button(frame_file, text="Browse...", command=self.browse_file).pack(side=tk.LEFT, padx=5)

        # Password Frame
        frame_pass = tk.LabelFrame(self.root, text="2. Enter Master Password", padx=10, pady=10)
        frame_pass.pack(fill="x", padx=10, pady=5)

        tk.Label(frame_pass, text="Password:").pack(side=tk.LEFT, padx=5)
        tk.Entry(frame_pass, textvariable=self.password, show="*", width=30).pack(side=tk.LEFT, padx=5)
        tk.Button(frame_pass, text="Decrypt", command=self.perform_decryption, bg="#dddddd").pack(side=tk.LEFT, padx=20)

        # Results Frame
        frame_results = tk.LabelFrame(self.root, text="3. Decrypted Content", padx=10, pady=10)
        frame_results.pack(fill="both", expand=True, padx=10, pady=5)

        # Scrollbars
        h_scroll = tk.Scrollbar(frame_results, orient=tk.HORIZONTAL)
        h_scroll.pack(side=tk.BOTTOM, fill=tk.X)

        v_scroll = tk.Scrollbar(frame_results)
        v_scroll.pack(side=tk.RIGHT, fill=tk.Y)

        self.text_area = tk.Text(frame_results, wrap=tk.NONE,
                                xscrollcommand=h_scroll.set,
                                yscrollcommand=v_scroll.set)
        self.text_area.pack(fill="both", expand=True)

        h_scroll.config(command=self.text_area.xview)
        v_scroll.config(command=self.text_area.yview)

        # Bottom Buttons
        frame_actions = tk.Frame(self.root, padx=10, pady=10)
        frame_actions.pack(fill="x")

        tk.Button(frame_actions, text="Export to CSV", command=self.export_csv).pack(side=tk.RIGHT, padx=5)
        tk.Button(frame_actions, text="Clear", command=self.clear_all).pack(side=tk.RIGHT, padx=5)

    def browse_file(self):
        filename = filedialog.askopenfilename(filetypes=[("XML files", "*.xml"), ("All files", "*.*")])
        if filename:
            self.file_path.set(filename)

    def perform_decryption(self):
        xml_file = self.file_path.get()
        pwd = self.password.get()

        if not xml_file:
            messagebox.showerror("Error", "Please select a file first.")
            return
        if not pwd:
            messagebox.showerror("Error", "Please enter the password.")
            return

        try:
            decryptor = OISafeDecryptor(xml_file)

            # 1. Decrypt Master Key
            master_key_hex = decryptor.decrypt_master_key(pwd)

            # 2. Decrypt Entries
            self.decrypted_data = decryptor.decrypt_entries(master_key_hex)

            # 3. Display
            self.display_results()
            messagebox.showinfo("Success", f"Successfully decrypted {len(self.decrypted_data)} entries.")

        except ValueError as e:
            messagebox.showerror("Decryption Failed", str(e))
        except Exception as e:
            messagebox.showerror("Error", f"An unexpected error occurred: {e}")
            print(e)

    def display_results(self):
        self.text_area.delete(1.0, tk.END)

        if not self.decrypted_data:
            self.text_area.insert(tk.END, "No entries found.")
            return

        # Use tab separation for better alignment with non-fixed width, but Text widget is fixed width font usually.
        # Let's use a wide format string.
        # Header
        header = f"{'Category':<20} | {'Description':<30} | {'Username':<25} | {'Password':<25} | {'Website':<40} | {'Note'}\n"
        self.text_area.insert(tk.END, header)
        self.text_area.insert(tk.END, "-" * 200 + "\n")

        for entry in self.decrypted_data:
            cat = entry.get('Category', '')
            desc = entry.get('Description', '')
            user = entry.get('Username', '')
            pwd = entry.get('Password', '')
            site = entry.get('Website', '')
            note = entry.get('Note', '')

            # Replace newlines in note for display?
            note = note.replace('\n', ' ')

            line = f"{cat:<20} | {desc:<30} | {user:<25} | {pwd:<25} | {site:<40} | {note}\n"
            self.text_area.insert(tk.END, line)

    def export_csv(self):
        if not self.decrypted_data:
            messagebox.showwarning("Warning", "No data to export.")
            return

        filename = filedialog.asksaveasfilename(defaultextension=".csv", filetypes=[("CSV files", "*.csv")])
        if filename:
            try:
                keys = ['Category', 'Description', 'Website', 'Username', 'Password', 'Note']
                with open(filename, 'w', newline='', encoding='utf-8') as f:
                    writer = csv.DictWriter(f, fieldnames=keys)
                    writer.writeheader()
                    writer.writerows(self.decrypted_data)
                messagebox.showinfo("Export Success", f"Data exported to {filename}")
            except Exception as e:
                messagebox.showerror("Export Failed", str(e))

    def clear_all(self):
        self.file_path.set("")
        self.password.set("")
        self.text_area.delete(1.0, tk.END)
        self.decrypted_data = []

if __name__ == "__main__":
    root = tk.Tk()
    app = DecryptionApp(root)
    root.mainloop()
