output "state_bucket_name" {
  description = "Name of the created Terraform state bucket."
  value       = aws_s3_bucket.terraform_state.id
}

output "terraform_state_operator_policy_arn" {
  description = "Attach this policy only to the approved Terraform operator identity."
  value       = aws_iam_policy.terraform_state_operator.arn
}
