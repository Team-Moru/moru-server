resource "aws_cloudwatch_log_group" "application" {
  name              = var.application_log_group_name
  retention_in_days = 14

  lifecycle {
    prevent_destroy = true
  }
}
