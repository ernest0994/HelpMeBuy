#!/usr/bin/env bash
set -euo pipefail

# This script runs when LocalStack is ready (mounted at /etc/localstack/init/ready.d)
# It will create the required AWS resources if they don't exist.

REGION="${AWS_DEFAULT_REGION:-us-east-1}"

log() { echo "[localstack-init] $*"; }

# Create DynamoDB table 'Lists' with numeric 'id' as HASH key
create_dynamodb_table() {
  if awslocal dynamodb describe-table --table-name Lists >/dev/null 2>&1; then
    log "DynamoDB table 'Lists' already exists. Skipping creation."
  else
    log "Creating DynamoDB table 'Lists'..."
    for attempt in {1..3}; do
      if awslocal dynamodb create-table \
        --table-name Lists \
        --attribute-definitions AttributeName=id,AttributeType=N \
        --key-schema AttributeName=id,KeyType=HASH \
        --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
        --region "$REGION"; then
        log "Waiting for table 'Lists' to be ACTIVE..."
        awslocal dynamodb wait table-exists --table-name Lists --region "$REGION"
        log "DynamoDB table 'Lists' is ready."
        return
      fi
      log "Attempt $attempt failed. Retrying..."
      sleep 2
    done
    log "Failed to create DynamoDB table 'Lists' after 3 attempts."
    exit 1
  fi
}

# Create S3 bucket 'helpmebuy-cache'
create_s3_bucket() {
  if awslocal s3api head-bucket --bucket helpmebuy-cache >/dev/null 2>&1; then
    log "S3 bucket 'helpmebuy-cache' already exists. Skipping creation."
  else
    log "Creating S3 bucket 'helpmebuy-cache'..."
    for attempt in {1..3}; do
      if awslocal s3 mb s3://helpmebuy-cache --region "$REGION"; then
        log "S3 bucket 'helpmebuy-cache' created."
        return
      fi
      log "Attempt $attempt failed. Retrying..."
      sleep 2
    done
    log "Failed to create S3 bucket 'helpmebuy-cache' after 3 attempts."
    exit 1
  fi
}

# Add test data to DynamoDB 'Lists' table (idempotent)
add_test_dynamodb_item() {
  if awslocal dynamodb get-item --table-name Lists --key '{"id": {"N": "1"}}' --region "$REGION" | grep -q '"name": {"S": "Test List"}'; then
    log "Test item (id=1) already exists in 'Lists'. Skipping."
  else
    log "Adding test item to 'Lists'..."
    awslocal dynamodb put-item \
      --table-name Lists \
      --item '{"id": {"N": "1"}, "name": {"S": "Test List"}}' \
      --region "$REGION"
    log "Test item added to 'Lists'."
  fi
}

# Add test data to S3 'helpmebuy-cache' bucket (idempotent)
add_test_s3_object() {
  if awslocal s3api head-object --bucket helpmebuy-cache --key milk.json >/dev/null 2>&1; then
    log "Test object 'milk.json' already exists in 'helpmebuy-cache'. Skipping."
  else
    log "Adding test object to 'helpmebuy-cache'..."
    echo '{"itemId": "milk", "store": "Walmart", "price": 2.50}' | awslocal s3 cp - s3://helpmebuy-cache/milk.json --region "$REGION"
    log "Test object 'milk.json' added to 'helpmebuy-cache'."
  fi
}

create_dynamodb_table
add_test_dynamodb_item
create_s3_bucket
add_test_s3_object

log "Initialization complete."
