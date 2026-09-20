# Terraform Infrastructure

`bootstrap` creates the dedicated S3 backend resources. It intentionally uses
local state for this one-time setup. `prod` manages the existing Moru
production infrastructure after its backend is configured.

## Safety Boundary

The current `moru-prod` AWS profile is read-only. It can run discovery,
`terraform fmt`, `terraform validate`, and a read-only import plan. It cannot
create the state bucket or import resources into remote state.

Do not run `terraform apply`, `terraform import`, or `terraform destroy` until
the plan is reviewed and explicit approval is given.

## State Backend

The backend is deliberately separate from the existing application buckets.
The bootstrap configuration creates a private bucket with:

- AES256 server-side encryption
- S3 bucket versioning for state recovery
- S3 lockfiles through `use_lockfile = true`
- public-access blocking and bucket-owner-enforced ownership
- a transport-security bucket policy
- an unattached least-privilege policy for the future Terraform operator

HashiCorp recommends bucket versioning for S3 state recovery and S3 lockfiles
for state locking. DynamoDB locking is not used because it is deprecated by the
S3 backend.

### Bootstrap after approval

1. Sign in with a separately approved AWS identity that can create the backend
   bucket and IAM policy. Do not use the root account.
2. Choose a globally unique state bucket name. The recommended candidate is
   `moru-prod-terraform-state-488230509502`, subject to availability.
3. Run the following from `infra/bootstrap`:

   ```bash
   terraform init
   terraform plan -var='state_bucket_name=CHOSEN_BUCKET_NAME'
   ```

4. Review that the plan only creates the state bucket resources and the
   unattached operator policy. After explicit approval, run `terraform apply`
   with the same variable.
5. Attach the created state-access policy only to the approved Terraform
   operator identity. Keep the `moru_terraform` discovery user read-only.

### Configure the production backend

1. Copy `infra/prod/backend.tf.example` to `infra/prod/backend.tf` and copy
   `infra/prod/backend.hcl.example` to `infra/prod/backend.hcl`.
2. Replace the bucket name with the approved state bucket name. Do not commit
   either file.
3. Run the following from `infra/prod`:

   ```bash
   terraform init -backend-config=backend.hcl
   terraform plan
   ```

The first production plan should show imports only. Any planned create, update,
replacement, or destroy for an existing production resource is a stop signal.

Before the state bucket exists, leave `backend.tf` absent and use Terraform's
local backend only for read-only validation. Local state and plan files are
ignored by Git and must never be committed.

## Non-Terraform Host Configuration

Docker Compose, Redis, GitHub Actions, Nginx, Certbot, DuckDNS, and EC2-hosted
files remain outside the Terraform scope in this phase. Their existing setup is
documented in the repository and on the production host; it must not be changed
as part of adoption.
