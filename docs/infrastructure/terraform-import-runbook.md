# Production Terraform Import Runbook

## Purpose

This runbook adopts existing Moru production AWS resources into Terraform
state. It must not create, update, replace, or delete an application resource.
The configuration uses declarative `import` blocks, so the import is reviewed
through the normal `plan` and `apply` workflow.

## Preconditions

1. The Terraform state bucket has been created from `infra/bootstrap` after a
   separate approval.
2. The approved Terraform operator has the state-access policy created by the
   bootstrap configuration, plus only the AWS permissions needed to manage the
   imported resources.
3. The `moru_terraform` discovery user remains read-only. Do not use it for
   import or apply.
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

## Read-Only Validation

The following commands do not write Terraform state and are safe to run with
the `moru_terraform` read-only profile after login:

```bash
cd infra/prod
AWS_PROFILE=moru-prod terraform init -reconfigure
AWS_PROFILE=moru-prod terraform validate
AWS_PROFILE=moru-prod terraform plan -lock=false
```

The expected result is a plan containing imports only. Stop immediately when
the plan includes an action other than `import` for an existing resource.

## Approved Import Procedure

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

7. Remove the local `prod-import.tfplan` file. It is ignored by Git and must
   never be committed.

## Stop Conditions

Stop and investigate before an apply when any of the following occurs:

- A plan proposes a create, change, replace, or destroy action for an existing
  production resource.
- The AWS account check is not `488230509502`.
- The state bucket cannot be encrypted, versioned, or locked.
- A resource ID differs from `docs/infrastructure/prod-inventory.md`.
- The application health check fails before or during the activity.

## Post-Import Follow-Up

The open RDS and SSH network rules are documented security findings, not an
incidental Terraform migration change. Address them in a separate, reviewed
security issue with an explicit connectivity test and rollback procedure.
