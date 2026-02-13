import os

file_path = "Safe/src/main/java/org/openintents/safe/Restore.java"

with open(file_path, "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "private void selectFileOrRestoreFromFile() {" in line:
        new_lines.append(line)
        # Skip the old implementation
        skip = True
        # Insert new implementation
        new_lines.append("        Intent intent = Intents.createOpenDocumentIntents(\"*/*\", PreferenceActivity.getBackupDocument(this));\n")
        new_lines.append("        try {\n")
        new_lines.append("            startActivityForResult(intent, REQUEST_RESTORE_DOCUMENT);\n")
        new_lines.append("        } catch (android.content.ActivityNotFoundException e) {\n")
        new_lines.append("            Toast.makeText(this, R.string.restore_error, Toast.LENGTH_LONG).show();\n")
        new_lines.append("        }\n")
        new_lines.append("    }\n")
        continue

    if skip:
        if line.strip() == "}":
            skip = False
        continue

    if "private boolean intentCallable(Intent intent) {" in line:
        skip = True
        continue

    new_lines.append(line)

with open(file_path, "w") as f:
    f.writelines(new_lines)
