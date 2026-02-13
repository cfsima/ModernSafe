# OI Safe Decryption Utility

This utility allows you to decrypt OI Safe backup files (`.xml`) on your computer.

## Requirements

*   Python 3.x
*   `pycryptodome` library
*   `tkinter` (usually included with Python)

## Installation

1.  Install Python 3 from [python.org](https://www.python.org/).
2.  Install the required crypto library:
    ```bash
    pip install pycryptodome
    ```
    *(Note: On some systems, you might need to use `pip3` instead of `pip`)*

## Usage

1.  Run the script:
    ```bash
    python decrypt_gui.py
    ```
2.  **Select Backup File**: Click "Browse..." and select your OI Safe XML backup file.
3.  **Enter Password**: Type your Master Password.
4.  **Decrypt**: Click the "Decrypt" button.
5.  **View Results**: The decrypted entries will appear in the text area below.
6.  **Export**: You can export the decrypted data to a CSV file by clicking "Export to CSV".

## Troubleshooting

*   **"ModuleNotFoundError: No module named 'Crypto'"**: Run `pip install pycryptodome`.
*   **"Decryption Failed: Incorrect Password or Corrupted Data"**: Ensure you are using the correct Master Password. Also, ensure the backup file is a valid OI Safe XML export.
*   **"Salt not found in XML"**: The selected file does not appear to be a valid OI Safe backup.

## Security Note

This script runs locally on your machine. Your password and data are processed only in memory and are not sent anywhere. When exporting to CSV, be aware that the CSV file will contain your passwords in plain text.
