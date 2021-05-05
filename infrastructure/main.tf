provider "azurerm" {
  version = "~> 2.25"
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
