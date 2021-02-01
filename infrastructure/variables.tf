variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "common_tags" {
  type = map(string)
}

variable "postgresql_database_name" {
  default = "wa_case_event_handler"
}

variable "postgresql_user" {
  default = "wa_ceh"
}
