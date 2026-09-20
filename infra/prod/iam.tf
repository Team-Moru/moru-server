data "aws_iam_policy_document" "moru_server_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

data "aws_iam_policy_document" "moru_dev_assets" {
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::moru-dev-assets-488230509502"]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["tts/*", "profiles/*"]
    }
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = [
      "arn:aws:s3:::moru-dev-assets-488230509502/tts/*",
      "arn:aws:s3:::moru-dev-assets-488230509502/profiles/*",
    ]
  }
}

data "aws_iam_policy_document" "moru_prod_assets" {
  statement {
    sid       = "AccessMoruAssetBucket"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::moru-prod-assets-488230509502"]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["tts/*", "profiles/*"]
    }
  }

  statement {
    sid    = "ManageMoruAssetObjects"
    effect = "Allow"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = [
      "arn:aws:s3:::moru-prod-assets-488230509502/tts/*",
      "arn:aws:s3:::moru-prod-assets-488230509502/profiles/*",
    ]
  }

  statement {
    sid    = "WriteMoruApplicationLogs"
    effect = "Allow"
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents",
    ]
    resources = ["arn:aws:logs:${var.aws_region}:${var.aws_account_id}:log-group:/moru/prod/app:*"]
  }
}

resource "aws_iam_role" "moru_server" {
  name                 = var.ec2_iam_role_name
  description          = "Allows EC2 instances to call AWS services on your behalf."
  assume_role_policy   = data.aws_iam_policy_document.moru_server_assume_role.json
  max_session_duration = 3600

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_iam_role_policy" "moru_dev_assets" {
  name   = "moru-dev-assets-policy"
  role   = aws_iam_role.moru_server.id
  policy = data.aws_iam_policy_document.moru_dev_assets.json
}

resource "aws_iam_role_policy" "moru_prod_assets" {
  name   = "moru-prod-assets-policy"
  role   = aws_iam_role.moru_server.id
  policy = data.aws_iam_policy_document.moru_prod_assets.json
}

resource "aws_iam_instance_profile" "moru_server" {
  name = "moru-server-s3-role"
  role = aws_iam_role.moru_server.name

  lifecycle {
    prevent_destroy = true
  }
}
