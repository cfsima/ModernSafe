import sys

with open("Safe/src/main/java/org/openintents/safe/PreferenceActivity.java", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
in_backup = False
in_export = False
skip_intent_callable = False
skip_ask_file_manager = False

for line in lines:
    stripped = line.strip()

    # Handle Backup Listener
    if "backupPathPref.setOnPreferenceClickListener(" in line:
        new_lines.append(line)
        in_backup = True
        skip = True # Skip the next lines (the lambda body)
        # Add new implementation
        new_lines.append("                    pref -> {\n")
        new_lines.append("                        Intent intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_BACKUP, OISAFE_XML);\n")
        new_lines.append("                        try {\n")
        new_lines.append("                            startActivityForResult(intent, REQUEST_BACKUP_FILENAME);\n")
        new_lines.append("                        } catch (android.content.ActivityNotFoundException e) {\n")
        new_lines.append("                            Toast.makeText(getActivity(), R.string.restore_error, Toast.LENGTH_LONG).show();\n")
        new_lines.append("                        }\n")
        new_lines.append("                        return false;\n")
        new_lines.append("                    }\n")
        continue

    if in_backup:
        # Check for closing of listener
        if stripped == ");":
            in_backup = False
            skip = False
            new_lines.append(line)
            continue
        if skip:
            continue

    # Handle Export Listener
    if "exportPathPref.setOnPreferenceClickListener(" in line:
        new_lines.append(line)
        in_export = True
        skip = True
        new_lines.append("                    pref -> {\n")
        new_lines.append("                        Intent intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_EXPORT, OISAFE_CSV);\n")
        new_lines.append("                        try {\n")
        new_lines.append("                            startActivityForResult(intent, REQUEST_EXPORT_FILENAME);\n")
        new_lines.append("                        } catch (android.content.ActivityNotFoundException e) {\n")
        new_lines.append("                            Toast.makeText(getActivity(), R.string.restore_error, Toast.LENGTH_LONG).show();\n")
        new_lines.append("                        }\n")
        new_lines.append("                        return false;\n")
        new_lines.append("                    }\n")
        continue

    if in_export:
        if stripped == ");":
            in_export = False
            skip = False
            new_lines.append(line)
            continue
        if skip:
            continue

    # Remove intentCallable method
    if "private boolean intentCallable(Intent intent) {" in line:
        skip_intent_callable = True
        continue

    if skip_intent_callable:
        if stripped == "}":
            skip_intent_callable = False
        continue

    # Remove askForFileManager method
    if "private void askForFileManager() {" in line:
        skip_ask_file_manager = True
        continue

    if skip_ask_file_manager:
        if stripped == "}":
            skip_ask_file_manager = False
        continue

    new_lines.append(line)

with open("Safe/src/main/java/org/openintents/safe/PreferenceActivity.java", "w") as f:
    f.writelines(new_lines)
