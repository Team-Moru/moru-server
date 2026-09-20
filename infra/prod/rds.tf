resource "aws_db_parameter_group" "moru" {
  name        = "korean"
  family      = "mysql8.4"
  description = "korean setting"

  parameter {
    name         = "character_set_client"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "character_set_connection"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "character_set_database"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "character_set_filesystem"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "character_set_results"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "character_set_server"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "collation_connection"
    value        = "utf8mb4_general_ci"
    apply_method = "immediate"
  }

  parameter {
    name         = "collation_server"
    value        = "utf8mb4_general_ci"
    apply_method = "immediate"
  }

  parameter {
    name         = "max_connections"
    value        = "150"
    apply_method = "immediate"
  }

  parameter {
    name         = "time_zone"
    value        = "Asia/Seoul"
    apply_method = "immediate"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_db_instance" "moru" {
  identifier     = var.rds_instance_identifier
  engine         = "mysql"
  engine_version = "8.4.9"
  instance_class = "db.t4g.micro"
  db_name        = "moru"
  port           = 3306

  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = true

  availability_zone                   = "ap-northeast-2a"
  db_subnet_group_name                = data.aws_db_subnet_group.existing.name
  vpc_security_group_ids              = [aws_security_group.moru_rds.id]
  parameter_group_name                = aws_db_parameter_group.moru.name
  option_group_name                   = "default:mysql-8-4"
  network_type                        = "IPV4"
  publicly_accessible                 = true
  multi_az                            = false
  deletion_protection                 = false
  copy_tags_to_snapshot               = true
  iam_database_authentication_enabled = false

  auto_minor_version_upgrade = true
  backup_retention_period    = 1
  backup_window              = "16:19-16:49"
  maintenance_window         = "sun:14:52-sun:15:22"
  apply_immediately          = false

  lifecycle {
    prevent_destroy = true
  }
}
