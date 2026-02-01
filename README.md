# Application de Chat en Ligne

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blue.svg)](https://stomp.github.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Application de chat en temps réel développée avec Spring Boot et WebSocket, suivant les principes de **Clean Architecture** et **SOLID**.

## Table des Matières

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Utilisation](#utilisation)
- [Tests](#tests)
- [Déploiement](#déploiement)
- [API Documentation](#api-documentation)
- [Contribuer](#contribuer)
- [Licence](#licence)

---

## Aperçu

Cette application permet à plusieurs utilisateurs de communiquer en temps réel via une interface web moderne et responsive. Elle utilise WebSocket avec le protocole STOMP pour garantir une communication bidirectionnelle instantanée.


##  Fonctionnalités

### Fonctionnalités Principales

-  **Chat en temps réel** avec WebSocket/STOMP
-  **Connexion simple** avec pseudo unique
-  **Envoi/réception** de messages instantanés
-  **Liste des utilisateurs** en ligne
-  **Notifications** de connexion/déconnexion
-  **Indicateur de frappe** (typing indicator)
-  **Historique des messages** récents
-  **Anti-spam** avec limitation de débit
-  **Interface responsive** (desktop/mobile)
-  **Validation** côté client et serveur

### Fonctionnalités Techniques

- ️ **Clean Architecture** (Domain, Application, Infrastructure, Presentation)
-  **Principes SOLID** appliqués rigoureusement
-  **Validation** et gestion d'erreurs robuste
-  **Logging** structuré avec SLF4J
-  **Tests unitaires** et d'intégration (JUnit 5, Mockito)
-  **DTOs** pour la communication inter-couches
-  **Reconnexion automatique** WebSocket

---

## ️ Architecture

L'application suit les principes de **Clean Architecture** avec une séparation claire des responsabilités :