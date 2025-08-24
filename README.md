# Help Me Buy

A cross-platform app (Android/iOS via Kotlin Multiplatform) to simplify grocery shopping by creating lists, comparing prices, and sharing with others. Built with Kotlin 2, Java 21, Jetpack Compose, Ktor, Room, Keycloak, Kafka, and AWS (free tier), with a fully local dev environment using Docker and LocalStack.

## Setup Instructions

### Prerequisites

- **Docker Desktop**: Free, required for LocalStack, Kafka, and Keycloak.
- **Python 3.12+**: For virtual environment to install `awscli-local`. Install via Homebrew: `brew install python@3.12`.
- **IntelliJ IDEA**: Community Edition or All Products Pack for KMP development.
- **Git**: For version control.

### Virtual Environment Setup

To use `awslocal` for LocalStack CLI commands, set up a Python virtual environment to avoid `externally-managed-environment` errors (PEP 668) and JetBrains Toolbox PATH conflicts.

1. **Create Virtual Environment**:
   ```bash
   cd ~/IdeaProjects/HelpMeBuy
   python3 -m venv venv
   ```

2. **Activate Virtual Environment**:
   ```bash
   source venv/bin/activate
   ```
    - Prompt shows `(venv)`.
    - Deactivate with: `deactivate`.

3. **Update pip**:
   ```bash
   pip install --upgrade pip
   ```

4. **Install `awscli-local`**:
   ```bash
   pip install awscli==1.34.16 awscli-local
   ```

5. **Verify**:
   ```bash
   awslocal --version
   ```
    - Expect: `aws-cli/1.34.16 Python/3.x.x ... localstack/awscli-local`.

6. **Optional: Add Alias**:
    - Add to `~/.zshrc` for quick activation:
      ```bash
      echo 'alias activate-venv="source ~/IdeaProjects/HelpMeBuy/venv/bin/activate"' >> ~/.zshrc
      source ~/.zshrc
      ```
    - Use: `activate-venv` to activate.

**Troubleshooting**:
- **If `awslocal` fails**: Reinstall:
  ```bash
  pip uninstall awscli awscli-local
  pip install awscli==1.34.16 awscli-local
  ```
- **JetBrains Toolbox Conflict**: Remove `/Users/ernestoacosta/Library/Application Support/JetBrains/Toolbox/scripts` from PATH in `~/.zshrc`, `~/.zprofile`, or `~/.zshenv`:
  ```bash
  nano ~/.zshrc
  # Comment out or remove Toolbox PATH line
  source ~/.zshrc
  ```
- **Python Version**: If `python3.13` has issues, use `python3.12`:
  ```bash
  brew install python@3.12
  python3.12 -m venv venv
  ```

## Local AWS Mocks (LocalStack)

LocalStack mocks AWS DynamoDB (`Lists` table for list storage) and S3 (`helpmebuy-cache` bucket for price caching) locally via Docker, keeping dev costs at $0.

**Services Provisioned** (via `localstack/init/10-init.sh`):
- DynamoDB table: `Lists` (HASH key: `id` as Number, test item: `id=1, name="Test List"`)
- S3 bucket: `helpmebuy-cache` (test object: `milk.json`)

**Prerequisites**:
- Docker Desktop (free, installed for HMB-1).
- `awscli-local` in virtual environment (see above).

**Start LocalStack**:
1. Ensure `docker-compose.yml` and `localstack/init/10-init.sh` are in project root.
2. Set script permissions:
   ```bash
   chmod +x localstack/init/10-init.sh
   ```
3. Start:
   ```bash
   docker-compose up -d
   ```

**Verify**:
1. Activate virtual environment:
   ```bash
   source venv/bin/activate
   ```
2. Check health:
   ```bash
   curl http://localhost:4566/_localstack/health
   ```
    - Expect: `{"services": {"dynamodb": "running", "s3": "running"}}` (or `"available"` for DynamoDB).
3. Check logs:
   ```bash
   docker-compose logs localstack
   ```
    - Expect:
        - `[localstack-init] Creating DynamoDB table 'Lists'...`
        - `[localstack-init] Creating S3 bucket 'helpmebuy-cache'...`
        - `[localstack-init] Initialization complete.`
4. Verify DynamoDB:
   ```bash
   awslocal dynamodb list-tables
   ```
    - Expect: `{"TableNames": ["Lists"]}`.
    - Check test data:
      ```bash
      awslocal dynamodb scan --table-name Lists
      ```
        - Expect: `{"Items": [{"id": {"N": "1"}, "name": {"S": "Test List"}}]}`.
5. Verify S3:
   ```bash
   awslocal s3 ls
   ```
    - Expect: `helpmebuy-cache`.
    - Check test data:
      ```bash
      awslocal s3 ls s3://helpmebuy-cache/
      ```
        - Expect: `milk.json`.
    - View:
      ```bash
      awslocal s3 cp s3://helpmebuy-cache/milk.json -
      ```
        - Expect: `{"itemId": "milk", "store": "Walmart", "price": 2.50}`.
6. Check container status:
   ```bash
   docker inspect helpmebuy-localstack --format '{{.State.Health.Status}}'
   ```
    - Expect: `healthy`.

**Alternative CLI (without `awslocal`)**:
- Use standard AWS CLI:
  ```bash
  aws --endpoint-url=http://localhost:4566 dynamodb list-tables --region us-east-1
  aws --endpoint-url=http://localhost:4566 s3 ls --region us-east-1
  ```

**Stop LocalStack**:
```bash
docker-compose down
```

**Notes**:
- Initialization script (`10-init.sh`) runs automatically via volume `./localstack/init:/etc/localstack/init/ready.d`, creating resources idempotently with retries.
- Data persists via `./localstack/data:/var/lib/localstack`.
- Default region: `us-east-1`.
- If resources missing, run manually:
  ```bash
  docker exec helpmebuy-localstack /etc/localstack/init/ready.d/10-init.sh
  ```
- If `awslocal` fails in container, install:
  ```bash
  docker exec helpmebuy-localstack pip install awscli-local
  ```

## Development Workflow
- **IDE**: IntelliJ IDEA (Community or All Products Pack).
- **Version Control**: GitHub repo with GitHub Actions for CI/CD.
- **Issue Tracking**: YouTrack for Kanban tasks.
- **Next Steps**: Configure Kafka, Keycloak, then build Lists Creation.

This is a Kotlin Multiplatform project targeting Android, iOS, Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you can add code to the
  platform-specific folders here too.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…