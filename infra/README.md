# Terraform Infrastructure

`bootstrap` created the dedicated S3 backend and retains local state. `prod`
manages the 30 existing Moru production AWS resources imported on 2026-09-24.
The post-import plan reported 0 to add, 0 to change, and 0 to destroy. See the
[import runbook](../docs/infrastructure/terraform-import-runbook.md) for the
original scope and verification record.

## State and Access

The production state is stored in the private
`moru-prod-terraform-state-488230509502` bucket at `prod/terraform.tfstate`.
The bucket uses AES256 encryption, versioning, public-access blocking, and S3
lockfiles. Keep state, plan files, and local backend configuration files out of
Git; state and plans may contain sensitive values. The local
`infra/bootstrap/terraform.tfstate` is also sensitive and must be backed up in
an access-controlled location outside the repository.

The `moru-prod` AWS CLI profile uses the `moru_terraform` IAM user. It has
production read access and the dedicated state-bucket policy, but not general
write access to EC2, RDS, or application S3 buckets. Grant only the specific
AWS write permissions required for an approved infrastructure change. Never
use the root account for Terraform operations.

On a new workstation, copy `infra/prod/backend.tf.example` to
`infra/prod/backend.tf` and `infra/prod/backend.hcl.example` to
`infra/prod/backend.hcl`. Set the approved bucket name in `backend.hcl`. Both
files are ignored by Git. Then initialize from `infra/prod` with
`AWS_PROFILE=moru-prod terraform init -reconfigure -backend-config=backend.hcl`.
Do not create another state bucket or repeat the production import.

## Production Changes

1. Change the Terraform configuration in a reviewed PR. Keep application
   deployment and host configuration outside this change unless separately
   approved.
2. From `infra/prod`, run `terraform fmt -check`, `terraform validate`, and
   `AWS_PROFILE=moru-prod terraform plan -out=prod.tfplan -lock-timeout=5m`.
   Review the exact plan, especially any replacement or deletion. Do not use
   `-lock=false` for production changes.
3. After explicit approval and with scoped AWS write permissions, apply the
   reviewed plan with `AWS_PROFILE=moru-prod terraform apply prod.tfplan`.
4. Run `AWS_PROFILE=moru-prod terraform plan -lock-timeout=5m` again and check
   for `No changes`. Do not commit the saved plan.

The existing GitHub Actions workflow builds and deploys the application; it
does not run Terraform. Terraform applies remain manual and approval-gated.

## Outside Terraform

The shared VPC, subnets, and DB subnet group are referenced as data sources.
Docker Compose, Redis, GitHub Actions, Nginx, Certbot, DuckDNS, and EC2-hosted
files remain outside Terraform management in this phase.
