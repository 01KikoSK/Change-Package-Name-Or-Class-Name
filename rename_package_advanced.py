import os
import re
import shutil
from pathlib import Path

def rename_package_advanced(root_dir: str, old_package_name: str, new_package_name: str, simulate: bool = True):
    """
    Renames a Python package within a given root directory, handling various complexities.

    Args:
        root_dir: The root directory of the project containing the package.
        old_package_name: The fully qualified name of the package to rename (e.g., 'com.example.old').
        new_package_name: The fully qualified name of the new package (e.g., 'org.newdomain.new').
        simulate: If True, performs a dry run and prints the changes without modifying files. Defaults to True.

    Returns:
        A dictionary containing lists of changed files and potential issues.
    """
    changed_files = []
    potential_issues = []

    old_parts = old_package_name.split('.')
    new_parts = new_package_name.split('.')

    old_path = Path(root_dir).joinpath(*old_parts)
    new_path = Path(root_dir).joinpath(*new_parts)

    if not old_path.is_dir():
        potential_issues.append(f"Error: Old package directory not found: {old_path}")
        return {"changed_files": changed_files, "potential_issues": potential_issues}

    if new_path.exists():
        potential_issues.append(f"Warning: New package directory already exists: {new_path}. This might lead to conflicts.")

    # Rename the directory
    if not simulate:
        try:
            new_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(old_path), str(new_path))
            changed_files.append(f"Directory renamed: {old_path} -> {new_path}")
        except OSError as e:
            potential_issues.append(f"Error renaming directory: {e}")
            return {"changed_files": changed_files, "potential_issues": potential_issues}
    else:
        changed_files.append(f"[SIMULATED] Directory renamed: {old_path} -> {new_path}")

    # Walk through all Python files in the root directory
    for root, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith(".py"):
                filepath = Path(root) / file
                original_content = filepath.read_text(encoding='utf-8', errors='ignore')
                new_content = original_content

                # Regex to find and replace import statements
                old_import_regex = r"from\s+" + re.escape(old_package_name) + r"(\.|\s|$)"
                new_import_replacement = f"from {new_package_name}\\1"
                new_content = re.sub(old_import_regex, new_import_replacement, new_content)

                old_import_regex_2 = r"import\s+" + re.escape(old_package_name) + r"(\.|\s|$)"
                new_import_replacement_2 = f"import {new_package_name}\\1"
                new_content = re.sub(old_import_regex_2, new_import_replacement_2, new_content)

                # Regex to find and replace references to the old package name (more complex)
                # This tries to avoid replacing substrings within other words
                old_ref_regex = r"(?<!\w)" + re.escape(old_package_name) + r"(?!\w)"
                new_ref_replacement = new_package_name
                new_content = re.sub(old_ref_regex, new_ref_replacement, new_content)

                if new_content != original_content:
                    changed_files.append(f"Modified file: {filepath}")
                    if not simulate:
                        try:
                            filepath.write_text(new_content, encoding='utf-8')
                        except OSError as e:
                            potential_issues.append(f"Error writing to file {filepath}: {e}")

    return {"changed_files": changed_files, "potential_issues": potential_issues}

def rename_class_advanced(root_dir: str, old_class_name: str, new_class_name: str, simulate: bool = True):
    """
    Renames a Python class within a given root directory, handling various complexities.

    Args:
        root_dir: The root directory of the project.
        old_class_name: The name of the class to rename.
        new_class_name: The new name for the class.
        simulate: If True, performs a dry run. Defaults to True.

    Returns:
        A dictionary containing lists of changed files and potential issues.
    """
    changed_files = []
    potential_issues = []

    class_definition_regex = r"class\s+" + re.escape(old_class_name) + r"\s*\(?"
    class_reference_regex = r"(?<!\w)" + re.escape(old_class_name) + r"(?!\w)"

    for root, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith(".py"):
                filepath = Path(root) / file
                original_content = filepath.read_text(encoding='utf-8', errors='ignore')
                new_content = original_content

                # Rename the class definition
                new_content = re.sub(class_definition_regex, f"class {new_class_name} (", new_content)
                new_content = re.sub(class_definition_regex.replace(r"\s*\(?", r""), f"class {new_class_name}", new_content) # Handle no parentheses

                # Rename references to the class (more cautious approach)
                # This tries to avoid renaming variables with similar names
                new_content = re.sub(class_reference_regex, new_class_name, new_content)

                if new_content != original_content:
                    changed_files.append(f"Modified file: {filepath}")
                    if not simulate:
                        try:
                            filepath.write_text(new_content, encoding='utf-8')
                        except OSError as e:
                            potential_issues.append(f"Error writing to file {filepath}: {e}")

    return {"changed_files": changed_files, "potential_issues": potential_issues}

if __name__ == "__main__":
    project_root = "/path/to/your/python/project"  # Replace with the actual path

    print("--- Renaming Package (Simulation) ---")
    package_rename_results_sim = rename_package_advanced(
        root_dir=project_root,
        old_package_name="my_old_package",
        new_package_name="com.new_org.my_new_package",
        simulate=True
    )
    print("Changed Files (Simulation):", package_rename_results_sim["changed_files"])
    print("Potential Issues (Simulation):", package_rename_results_sim["potential_issues"])

    input("\nPress Enter to proceed with actual package renaming (will modify files)...")

    print("\n--- Renaming Package (Actual) ---")
    package_rename_results_actual = rename_package_advanced(
        root_dir=project_root,
        old_package_name="my_old_package",
        new_package_name="com.new_org.my_new_package",
        simulate=False
    )
    print("Changed Files (Actual):", package_rename_results_actual["changed_files"])
    print("Potential Issues (Actual):", package_rename_results_actual["potential_issues"])

    print("\n--- Renaming Class (Simulation) ---")
    class_rename_results_sim = rename_class_advanced(
        root_dir=project_root,
        old_class_name="OldClassName",
        new_class_name="NewClassName",
        simulate=True
    )
    print("Changed Files (Simulation):", class_rename_results_sim["changed_files"])
    print("Potential Issues (Simulation):", class_rename_results_sim["potential_issues"])

    input("\nPress Enter to proceed with actual class renaming (will modify files)...")

    print("\n--- Renaming Class (Actual) ---")
    class_rename_results_actual = rename_class_advanced(
        root_dir=project_root,
        old_class_name="OldClassName",
        new_class_name="NewClassName",
        simulate=False
    )
    print("Changed Files (Actual):", class_rename_results_actual["changed_files"])
    print("Potential Issues (Actual):", class_rename_results_actual["potential_issues"])