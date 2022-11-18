terraform {
  required_providers {
    azurerm = {
      version = "=3.0.0"
    }
  }
}

provider "azurerm" {
  features {}
}

data "azurerm_key_vault" "wa_key_vault" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault" "ccd_key_vault" {
  name                = "ccd-${var.env}"
  resource_group_name = "ccd-shared-${var.env}"
}

//Retrieve and copy secret from s2s vault into wa-vault
data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name        = "microservicekey-wa-case-event-handler"
}

resource "azurerm_key_vault_secret" "s2s_secret_case_event_handler" {
  name         = "s2s-secret-case-event-handler"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

//Retrieve and copy secret from ccd vault into wa-vault
data "azurerm_key_vault_secret" "ccd_shared_servicebus_secret" {
  key_vault_id = data.azurerm_key_vault.ccd_key_vault.id
  name         = "ccd-servicebus-connection-string"
}

resource "azurerm_key_vault_secret" "ccd_shared_servicebus_connection_string" {
  name         = "ccd-shared-servicebus-connection-string"
  value        = data.azurerm_key_vault_secret.ccd_shared_servicebus_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

////////////////////////////////
// Create DB                  //
////////////////////////////////

module "wa_case_event_handler_database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}"
  name               = "${var.postgres_db_component_name}-postgres-db"
  location           = "${var.location}"
  env                = "${var.env}"
  database_name      = "${var.postgresql_database_name}"
  postgresql_user    = "${var.postgresql_user}"
  postgresql_version = "11"
  common_tags        = "${merge(var.common_tags, tomap("lastUpdated", "${timestamp()}"))}"
  subscription       = "${var.subscription}"
}


// Save secrets in vault
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.postgres_db_component_name}-POSTGRES-USER"
  value        = module.wa_case_event_handler_database.user_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PASS"
  value        = module.wa_case_event_handler_database.postgresql_password
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.postgres_db_component_name}-POSTGRES-HOST"
  value        = module.wa_case_event_handler_database.host_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PORT"
  value        = module.wa_case_event_handler_database.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-DATABASE"
  value        = module.wa_case_event_handler_database.postgresql_database
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}
