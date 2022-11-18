variable "product" {
  default = "wa"
}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {
  type        = string
  default     = ""
  description = "Deployment Namespace. Optional (only used in PRs)"
}

variable "common_tags" {
  type = map(string)
}

variable "postgres_db_component_name" {
  default = "wa-case-event-handler"
}

variable "postgresql_database_name" {
  default = "wa_case_event_messages_db"
}

variable "postgresql_user" {
  default = "wa_wa"
}
