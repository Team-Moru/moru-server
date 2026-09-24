# Production Infrastructure Inventory

## Purpose

This document records the existing production infrastructure before it is
adopted by Terraform. It is an inventory, not an authorization to change AWS
resources.

## Scope

| Area | Existing resource | Terraform approach |
| --- | --- | --- |
| EC2 | `i-0b4fe94824ca7d3c7` (`moru-server`) | Import and manage |
| Elastic IP | `eipalloc-0423074c40c068102` | Import and manage |
| EC2 security group | `sg-0ff632924dad2adb6` (`moru-ec2-sg`) | Import and manage |
| RDS MySQL | `moru-db` | Import and manage |
| RDS security group | `sg-0056408c9dc50cef4` (`moru-rds-sg`) | Import and manage |
| S3 assets | `moru-prod-assets-488230509502` | Import and manage |
| S3 TTS previews | `moru-prod-preview-assets-488230509502` | Import and manage |
| EC2 IAM role/profile | `moru-server-s3-role` | Import and manage |
| CloudWatch Logs | `/moru/prod/app` | Import and manage |
| VPC | `vpc-087c2d76939191694` | Reference with data source |
| EC2 subnet | `subnet-0a94acc68b254e59f` | Reference with data source |
| RDS subnet group | `default-vpc-087c2d76939191694` | Reference with data source |

The VPC and subnets are the account's default network and may be shared by
other workloads. They must not be imported or changed in this migration.

## Observed Production Configuration

### Compute and network

- EC2: `t3.micro`, Amazon Linux AMI `ami-0e4ab31f1847c850c`, in
  `ap-northeast-2c`.
- The instance uses the `moru-key` key pair, the `moru-server-s3-role` instance
  profile, and an IMDSv2-required metadata configuration.
- Elastic IP `43.202.84.114` is associated with the instance.
- The application container publishes only to `127.0.0.1:8080`; Nginx remains
  host-managed and proxies the public HTTP(S) traffic.

### Database

- RDS MySQL `moru-db`: MySQL `8.4.9`, `db.t4g.micro`, 20 GiB `gp2`, encrypted
  storage, one-day backup retention, and no Multi-AZ deployment.
- The database name is `moru`. Credentials are intentionally excluded from this
  repository and Terraform configuration.

### Storage and logging

- `moru-prod-assets-488230509502` stores private application assets.
- `moru-prod-preview-assets-488230509502` allows public reads only below
  `tts/previews/*` and `tts/common/*` for preview and common TTS audio.
- Both production buckets use AES256 default encryption. Versioning is not
  currently enabled.
- Docker uses the `awslogs` driver and writes to `/moru/prod/app`, which retains
  logs for 14 days. No CloudWatch metric alarms are currently configured.
- The EC2 role has inline policies for the production and development asset
  buckets. The production policy allows only the `tts/*` and `profiles/*`
  prefixes and CloudWatch log writes.

## Out of Scope for This Migration

- Docker Compose services, the Redis container, and the GitHub Actions deploy
  workflow remain unchanged.
- Nginx, Certbot, DuckDNS, and files on the EC2 host are documented only. They
  are not changed or managed by Terraform in this phase.
- Database credentials, application secrets, and GCP credentials remain in the
  existing secret-management flow.

## Security Review Items

These items were observed during the read-only audit. They are intentionally
not changed by the Terraform adoption work and need separate approval.

1. The RDS instance is publicly accessible, and `moru-rds-sg` currently allows
   all traffic from IPv4 and IPv6 addresses.
2. `moru-ec2-sg` allows SSH from every IPv4 address.
3. The production S3 buckets do not currently use versioning. This is distinct
   from the planned Terraform state bucket, which must use versioning.

## Adoption Safety Rules

- Never run `terraform apply`, `terraform import`, or `terraform destroy`
  without explicit approval.
- Do not accept a plan that replaces, creates, modifies, or destroys an
  existing production resource during import.
- Keep Terraform state, plan files, credentials, and application secrets out of
  Git.
