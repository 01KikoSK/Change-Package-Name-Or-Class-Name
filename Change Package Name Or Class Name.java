import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RenameCodeElementsAdvancedNoSimulation {

    public static class RenameResult {
        private final List<String> changedFiles;
        private final List<String> potentialIssues;

        public RenameResult() {
            this.changedFiles = new ArrayList<>();
            this.potentialIssues = new ArrayList<>();
        }

        public List<String> getChangedFiles() {
            return changedFiles;
        }

        public List<String> getPotentialIssues() {
            return potentialIssues;
        }

        public void addChangedFile(String file) {
            this.changedFiles.add(file);
        }

        public void addPotentialIssue(String issue) {
            this.potentialIssues.add(issue);
        }

        public boolean hasIssues() {
            return !this.potentialIssues.isEmpty();
        }

        public void merge(RenameResult other) {
            this.changedFiles.addAll(other.changedFiles);
            this.potentialIssues.addAll(other.potentialIssues);
        }
    }

    public static RenameResult renamePackage(String rootDir, String oldPackageName, String newPackageName) throws IOException {
        RenameResult result = new RenameResult();
        Path oldPackagePath = Paths.get(rootDir, oldPackageName.replace('.', File.separatorChar));
        Path newPackagePath = Paths.get(rootDir, newPackageName.replace('.', File.separatorChar));

        if (!Files.exists(oldPackagePath) || !Files.isDirectory(oldPackagePath)) {
            result.addPotentialIssue("Error: Old package directory not found: " + oldPackagePath);
            return result;
        }

        if (Files.exists(newPackagePath)) {
            result.addPotentialIssue("Warning: New package directory already exists: " + newPackagePath + ". This might lead to conflicts.");
        } else {
            Files.createDirectories(newPackagePath.getParent());
            Files.move(oldPackagePath, newPackagePath);
            result.addChangedFile("Directory renamed: " + oldPackagePath + " -> " + newPackagePath);
        }

        try (Stream<Path> javaFiles = Files.walk(Paths.get(rootDir))) {
            List<Path> filesToProcess = javaFiles
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path filePath : filesToProcess) {
                String content = Files.readString(filePath);
                String newContent = content;
                boolean fileChanged = false;

                // Update package declaration
                Pattern packagePattern = Pattern.compile("^package\\s+" + Pattern.quote(oldPackageName) + "\\s*;");
                Matcher packageMatcher = packagePattern.matcher(newContent);
                if (packageMatcher.find()) {
                    newContent = packageMatcher.replaceFirst("package " + newPackageName + ";");
                    if (!content.equals(newContent)) {
                        Files.writeString(filePath, newContent);
                        result.addChangedFile("Modified package declaration in: " + filePath);
                        content = newContent; // Update content for subsequent replacements
                        fileChanged = true;
                    }
                }

                // Update import statements
                Pattern importPattern = Pattern.compile("import\\s+" + Pattern.quote(oldPackageName) + "\\.([^;]+);");
                Matcher importMatcher = importPattern.matcher(content);
                StringBuffer sbImport = new StringBuffer();
                boolean importChanged = false;
                while (importMatcher.find()) {
                    importMatcher.appendReplacement(sbImport, "import " + newPackageName + "." + importMatcher.group(1) + ";");
                    importChanged = true;
                }
                importMatcher.appendTail(sbImport);
                if (importChanged) {
                    String tempContent = sbImport.toString();
                    if (!content.equals(tempContent)) {
                        Files.writeString(filePath, tempContent);
                        result.addChangedFile("Modified import statements in: " + filePath);
                        content = tempContent; // Update content for subsequent replacements
                        fileChanged = true;
                    }
                }

                // Update fully qualified name references (more complex)
                Pattern fqnPattern = Pattern.compile("(?<!\\w)" + Pattern.quote(oldPackageName) + "\\.(\\w+)");
                Matcher fqnMatcher = fqnPattern.matcher(content);
                StringBuffer sbFqn = new StringBuffer();
                boolean fqnChanged = false;
                while (fqnMatcher.find()) {
                    fqnMatcher.appendReplacement(sbFqn, newPackageName + "." + fqnMatcher.group(1));
                    fqnChanged = true;
                }
                fqnMatcher.appendTail(sbFqn);
                if (fqnChanged) {
                    String finalContent = sbFqn.toString();
                    if (!content.equals(finalContent)) {
                        Files.writeString(filePath, finalContent);
                        result.addChangedFile("Modified fully qualified name references in: " + filePath);
                        fileChanged = true;
                    }
                }
            }
        }

        return result;
    }

    public static RenameResult renameClass(String rootDir, String oldClassName, String newClassName) throws IOException {
        RenameResult result = new RenameResult();
        Pattern classDefinitionPattern = Pattern.compile("(?<!\\w)class\\s+" + Pattern.quote(oldClassName) + "\\s+");
        Pattern classReferencePattern = Pattern.compile("(?<!\\w)" + Pattern.quote(oldClassName) + "(?!\\w)");
        Pattern constructorReferencePattern = Pattern.compile("(?<!\\w)new\\s+" + Pattern.quote(oldClassName) + "\\s*\\(");

        try (Stream<Path> javaFiles = Files.walk(Paths.get(rootDir))) {
            List<Path> filesToProcess = javaFiles
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path filePath : filesToProcess) {
                String content = Files.readString(filePath);
                String newContent = content;
                boolean fileChanged = false;

                // Rename class definition
                Matcher definitionMatcher = classDefinitionPattern.matcher(newContent);
                if (definitionMatcher.find()) {
                    newContent = definitionMatcher.replaceFirst("class " + newClassName + " ");
                    if (!content.equals(newContent)) {
                        Files.writeString(filePath, newContent);
                        result.addChangedFile("Modified class definition in: " + filePath);
                        content = newContent; // Update content for subsequent replacements
                        fileChanged = true;
                    }
                }

                // Rename class references
                Matcher referenceMatcher = classReferencePattern.matcher(content);
                StringBuffer sbReference = new StringBuffer();
                boolean referenceFound = false;
                while (referenceMatcher.find()) {
                    referenceMatcher.appendReplacement(sbReference, newClassName);
                    referenceFound = true;
                }
                referenceMatcher.appendTail(sbReference);
                if (referenceFound) {
                    newContent = sbReference.toString();
                    if (!content.equals(newContent)) {
                        Files.writeString(filePath, newContent);
                        result.addChangedFile("Modified class references in: " + filePath);
                        content = newContent; // Update content for subsequent replacements
                        fileChanged = true;
                    }
                }

                // Rename constructor references
                Matcher constructorMatcher = constructorReferencePattern.matcher(content);
                StringBuffer sbConstructor = new StringBuffer();
                boolean constructorFound = false;
                while (constructorMatcher.find()) {
                    constructorMatcher.appendReplacement(sbConstructor, "new " + newClassName + " (");
                    constructorFound = true;
                }
                constructorMatcher.appendTail(sbConstructor);
                if (constructorFound) {
                    newContent = sbConstructor.toString();
                    if (!content.equals(newContent)) {
                        Files.writeString(filePath, newContent);
                        result.addChangedFile("Modified constructor references in: " + filePath);
                        fileChanged = true;
                    }
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        String rootDirectory = "/path/to/your/java/project"; // Replace with the actual root directory
        String oldPackage = "com.example.oldpackage";
        String newPackage = "org.newdomain.newpackage";
        String oldClass = "OldClassName";
        String newClass = "NewClassName";

        System.out.println("--- Performing Package Rename ---");
        RenameResult packageResult = renamePackage(rootDirectory, oldPackage, newPackage);
        System.out.println("Changed files: " + packageResult.getChangedFiles());
        System.out.println("Potential issues: " + packageResult.getPotentialIssues());

        System.out.println("\n--- Performing Class Rename ---");
        RenameResult classResult = renameClass(rootDirectory, oldClass, newClass);
        System.out.println("Changed files: " + classResult.getChangedFiles());
        System.out.println("Potential issues: " + classResult.getPotentialIssues());
    }
}