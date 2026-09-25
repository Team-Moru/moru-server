# Production Terraform Import Runbook

## Purpose

This runbook adopts existing Moru production AWS resources into Terraform
state. It must not create, update, replace, or delete an application resource.
The configuration uses declarative `import` blocks, so the import is reviewed
through the normal `plan` and `apply` workflow.

## Completion Record

The production import was completed on 2026-09-24 using the `moru-prod` AWS
profile in account `488230509502`. The reviewed plan contained 30 imports and
no additions, changes, or deletions. All 30 managed resources appeared in the
remote state afterward, and a fresh plan reported `No changes`.

The backend is the private `moru-prod-terraform-state-488230509502` bucket,
with state at `prod/terraform.tfstate`. Do not reapply the saved import plan or
repeat this procedure against the populated state. For future changes, follow
the approval workflow in [infra/README.md](../../infra/README.md).

## Historical Preconditions

1. The Terraform state bucket has been created from `infra/bootstrap` after a
   separate approval.
2. The `moru_terraform` IAM user has production read access and the dedicated
   state-access policy. This allowed the import-only apply to update state
   without changing the imported AWS resources.
3. Future infrastructure changes require separately approved, scoped AWS
   write permissions; the state-access policy alone is not sufficient.
4. No concurrent Terraform operation is running for prod.
5. The production service is healthy before beginning. Record the current
   health result and do not restart Docker, Nginx, or EC2 as part of this work.

## Import Order

The import blocks in `infra/prod/imports.tf` cover the following existing
resources:

1. EC2 security group and its seven rules
2. EC2 IAM role, two inline policies, and instance profile
3. EC2 instance and its Elastic IP
4. RDS security group and its three rules
5. RDS parameter group and MySQL instance
6. Private production assets S3 bucket and its security settings
7. Public TTS preview S3 bucket, its security settings, and its public-read
   bucket policy
8. CloudWatch application log group

The default VPC, default subnets, default DB subnet group, Docker Compose,
Redis, GitHub Actions, Nginx, Certbot, DuckDNS, and EC2-hosted files are not
imported in this phase.

## Current Validation

The following commands validate the configuration and check for drift using
the `moru-prod` profile after login:

```bash
cd infra/prod
AWS_PROFILE=moru-prod terraform init -reconfigure -backend-config=backend.hcl
AWS_PROFILE=moru-prod terraform validate
AWS_PROFILE=moru-prod terraform plan -lock-timeout=5m
```

The expected result now is `No changes`. Any proposed create, update,
replacement, or deletion needs investigation and approval before an apply.

## Historical Import Procedure (Completed)

The steps below record the one-time migration. Do not run them again for this
production state.

1. Use the approved Terraform operator profile and configure
   `infra/prod/backend.tf` and `infra/prod/backend.hcl` from the committed
   examples.
2. Initialize the remote state backend:

   ```bash
   terraform init -reconfigure -backend-config=backend.hcl
   ```

3. Produce a reviewable plan:

   ```bash
   terraform plan -out=prod-import.tfplan -lock-timeout=5m
   terraform show -no-color prod-import.tfplan
   ```

4. Confirm every action is an import and no existing resource has a planned
   update, replacement, or deletion. Obtain explicit approval for the exact
   plan.
5. Apply the reviewed plan once:

   ```bash
   terraform apply prod-import.tfplan
   ```

6. Verify Terraform now reports no drift:

   ```bash
   terraform plan -lock-timeout=5m
   terraform state list
   ```

7. Keep the local `prod-import.tfplan` file out of Git and remove it when no
   longer needed; saved plans can contain sensitive values.

## Stop Conditions

Stop and investigate before an apply when any of the following occurs:

- A plan proposes a create, change, replace, or destroy action for an existing
  production resource.
- The AWS account check is not `488230509502`.
- The state bucket cannot be encrypted, versioned, or locked.
- A resource ID differs from `docs/infrastructure/prod-inventory.md`.
- The application health check fails before or during the activity.

## Post-Import Follow-Up

Keep an access-controlled backup of the local
`infra/bootstrap/terraform.tfstate`, which manages the state bucket itself.
The application deployment workflow does not run Terraform.

The open RDS and SSH network rules are documented security findings, not an
incidental Terraform migration change. Address them in a separate, reviewed
security issue with an explicit connectivity test and rollback procedure.
