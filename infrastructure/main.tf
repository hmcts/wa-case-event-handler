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

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name        = "microservicekey-wa-case-event-handler"
}

resource "azurerm_key_vault_secret" "s2s_secret_case_event_handler" {
  name         = "s2s-secret-case-event-handler"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

// Azure service bus to be moved to shared infra

locals {
  topic_name        = "${var.product}-case-event-topic-${var.env}"
  subscription_name = "${var.product}-case-event-subscription-${var.env}"
  servicebus_namespace_name       = "${var.product}-servicebus-${var.env}"
  resource_group_name             = "${var.product}-${var.env}"
  tags = merge(
      var.common_tags,
      map(
        "Team Contact", "#wa-tech",
        "Team Name", "WA Team"
      )
    )
}


data "template_file" "subscription_template" {
  template = "${file("${path.module}/templates/subscription_template.json")}"
}
//Create namespace
module "servicebus-namespace" {
  source              = "git@github.com:hmcts/terraform-module-servicebus-namespace?ref=master"
  name                = local.servicebus_namespace_name
  location            = var.location
  resource_group_name = local.resource_group_name
  env                 = var.env
  common_tags         = local.tags
  sku                 = "Premium"
}

//Create topic
module "wa-case-event-handler-topic" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-topic?ref=master"
  name                  = local.topic_name
  namespace_name        = local.servicebus_namespace_name
  resource_group_name   = local.resource_group_name
}

//Create subscription
module "wa-case-event-handler-subscription" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-subscription?ref=master"
  template_body         = data.template_file.subscription_template.rendered
  name                  = local.subscription_name
  namespace_name        = local.servicebus_namespace_name
  resource_group_name   = local.resource_group_name
  topic_name            = local.topic_name
}
