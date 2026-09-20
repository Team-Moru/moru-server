resource "aws_s3_bucket" "production_assets" {
  bucket        = var.production_assets_bucket_name
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "production_assets" {
  bucket = aws_s3_bucket.production_assets.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "production_assets" {
  bucket = aws_s3_bucket.production_assets.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "production_assets" {
  bucket = aws_s3_bucket.production_assets.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }

    bucket_key_enabled = true
  }
}

data "aws_iam_policy_document" "production_preview_public_read" {
  statement {
    sid    = "AllowPublicReadTtsAudio"
    effect = "Allow"

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    actions = ["s3:GetObject"]

    resources = [
      "arn:aws:s3:::${var.production_preview_bucket_name}/tts/previews/*",
      "arn:aws:s3:::${var.production_preview_bucket_name}/tts/common/*",
    ]
  }
}

resource "aws_s3_bucket" "production_preview" {
  bucket        = var.production_preview_bucket_name
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "production_preview" {
  bucket = aws_s3_bucket.production_preview.id

  block_public_acls       = true
  block_public_policy     = false
  ignore_public_acls      = true
  restrict_public_buckets = false
}

resource "aws_s3_bucket_ownership_controls" "production_preview" {
  bucket = aws_s3_bucket.production_preview.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "production_preview" {
  bucket = aws_s3_bucket.production_preview.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }

    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_policy" "production_preview" {
  bucket = aws_s3_bucket.production_preview.id
  policy = data.aws_iam_policy_document.production_preview_public_read.json
}
