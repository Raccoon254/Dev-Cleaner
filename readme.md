# Dev Cleaner

### Streamlining Your Development Workspace

Dev Cleaner is a JavaFX application designed to help developers manage their workspace efficiently by deleting unnecessary files and directories such as `node_modules/`, `target/`, and other build artifacts. It helps free up disk space and keeps your system clutter-free.

## Features
- **Automatic Detection**: Identifies common development-related directories.
- **Confirmation Before Cleanup**: Prompt to confirm deletion, ensuring important files aren't removed.
- **Background Operation**: Set up Dev Cleaner to run periodically for hassle-free maintenance.
- **Detailed Logs**: Keep track of what was deleted and when.

## Technologies
- **JavaFX** for UI
- **Java 17**
- **Maven** for dependency management

## Running Dev Cleaner
To run the application, simply execute the following Maven command:
```bash
mvn javafx:run
```

## Contribution
Feel free to fork this repository, submit issues, and make pull requests. Contributions are always welcome!

## License
This project is licensed under the MIT License.
