@echo off
setlocal enabledelayedexpansion

:: Ask the user for inputs
set /p oldName="Enter the old package/class name to replace: "
set /p newName="Enter the new package/class name: "
set /p targetDir="Enter the target directory (e.g., C:\Project\Source): "

:: Confirm the user inputs
echo Replacing all occurrences of "%oldName%" with "%newName%" in files under "%targetDir%"
pause

:: Loop through all files in the target directory (and subdirectories)
for /r "%targetDir%" %%f in (*) do (
    echo Processing file: %%f
    
    :: Check if the file contains the old name
    findstr /m /i /c:"%oldName%" "%%f" >nul
    if !errorlevel! equ 0 (
        :: Replace occurrences of the old name with the new name using PowerShell
        powershell -Command "(Get-Content -Raw '%%f') -replace '%oldName%', '%newName%' | Set-Content '%%f'"
        echo Updated file: %%f
    )
)

echo Task complete! All occurrences have been replaced.
pause