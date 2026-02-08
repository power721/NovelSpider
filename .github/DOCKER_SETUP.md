# Docker Hub Integration Setup

This guide explains how to set up automatic Docker image publishing to Docker Hub via GitHub Actions.

## Prerequisites

1. A Docker Hub account
2. A repository on Docker Hub (optional - will be created automatically)

## Step-by-Step Setup

### 1. Create Docker Hub Access Token

1. Log in to [Docker Hub](https://hub.docker.com/)
2. Click your username → **Account Settings**
3. Select **Security** → **New Access Token**
4. Enter a description (e.g., "GitHub Actions")
5. Set permissions: **Read, Write, Delete**
6. Click **Generate**
7. **Important**: Copy the token immediately (you won't see it again!)

### 2. Configure GitHub Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following secrets:

   | Name | Secret |
   |------|--------|
   | `DOCKER_USERNAME` | Your Docker Hub username |
   | `DOCKER_PASSWORD` | The access token from Step 1 |

### 3. Update Workflows (Optional)

The workflows are configured to use `YOUR_USERNAME` as the placeholder. Update `.github/workflows/build-native.yml`:

```yaml
- name: Extract metadata
  id: meta
  uses: docker/metadata-action@v5
  with:
    images: YOUR_USERNAME/novelspider  # Replace with your Docker Hub username
```

Replace `YOUR_USERNAME` with your actual Docker Hub username.

### 4. Verify Setup

1. Push a commit to the main branch
2. Go to **Actions** tab in GitHub
3. Click on the "Build Native Image" workflow run
4. Check the "Build and push Docker image" step

If successful, you'll see:
- Docker image built and pushed to Docker Hub
- Image tagged with commit SHA and branch name
- Latest tag updated on main branch

## Docker Image Usage

### Pull the Image

```bash
docker pull YOUR_USERNAME/novelspider:latest-native
```

### Run the Container

```bash
docker run -d \
  -p 3000:3000 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/novels \
  -e SPRING_DATASOURCE_USERNAME=novel \
  -e SPRING_DATASOURCE_PASSWORD=password \
  YOUR_USERNAME/novelspider:latest-native
```

### Using Docker Compose

```yaml
services:
  novelspider:
    image: YOUR_USERNAME/novelspider:latest-native
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/novels
      SPRING_DATASOURCE_USERNAME: novel
      SPRING_DATASOURCE_PASSWORD: password
    ports:
      - "3000:3000"
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: novels
      MYSQL_USER: novel
      MYSQL_PASSWORD: password
      MYSQL_ROOT_PASSWORD: rootpassword
```

## Available Tags

The workflow automatically creates these tags:

- `latest-native` - Latest build from main/master branch
- `v1.2.3-native` - Semantic version tags
- `main-native` - Branch name
- `commit-hash-native` - Git commit SHA

## Troubleshooting

### Authentication Failed

- Verify `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets
- Check that access token has **Write** permissions
- Ensure token hasn't expired

### Repository Not Found

- Docker Hub will create the repository automatically on first push
- Verify your Docker Hub username is correct in the workflow

### Build Timeout

- Native builds can take 20-30 minutes
- Increase `timeout-minutes` in workflow if needed
- Check build logs for stuck steps

### Image Too Large

- Native images are typically 80-120MB compressed
- Ensure you're using the multi-stage Dockerfile
- Verify native image was built successfully

## Security Best Practices

1. **Use Access Tokens**: Never use your actual Docker Hub password
2. **Limit Permissions**: Only grant necessary permissions
3. **Rotate Tokens**: Regenerate tokens periodically
4. **Monitor Usage**: Check Docker Hub for unauthorized access
5. **Private Images**: Make private repositories if needed

## Alternative Registries

To use a different container registry (GitHub Container Registry, AWS ECR, etc.), modify the workflow:

```yaml
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

Then update the image name in the metadata step.
