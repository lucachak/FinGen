# 💸 FinGen

> Uma aplicação web inteligente e otimizada para a gestão financeira doméstica e controle de despesas.

[![GitHub Repo](https://img.shields.io/badge/GitHub-Repository-black?style=for-the-badge&logo=github)](https://github.com/lucachak/FinGen)

O **FinGen** foi desenvolvido para simplificar a vida financeira familiar. Ele oferece um ambiente seguro e intuitivo para o rastreamento de receitas, despesas e planejamento do orçamento doméstico, combinando um frontend reativo com um backend robusto e fácil de escalar graças à sua arquitetura em contêineres.

---

## ✨ Funcionalidades Principais

* **Gestão de Despesas e Receitas:** Registro rápido e categorização de todas as movimentações financeiras da casa.
* **Dashboard de Controle:** Visão geral clara do saldo, gastos mensais e economia, facilitando a tomada de decisão.
* **Autenticação Segura:** Sistema de login e controle de acesso integrado, garantindo a privacidade dos dados financeiros da família.
* **Arquitetura RESTful:** Comunicação ágil e padronizada entre a interface de usuário e o servidor.

---

## 🛠️ Tecnologias Utilizadas

A stack foi escolhida com foco em produtividade, segurança e facilidade de deploy, eliminando a necessidade de configurações complexas manuais.

### **Frontend**
* **React:** Interface dinâmica, componentizada e de carregamento rápido.

### **Backend**
* **Java & Spring Boot:** API REST ágil e escalável.
* **Spring Security / Auth nativo:** Simplificação radical da segurança, evitando centenas de linhas de código manual para proteger rotas e validar usuários, com integração fluida com o frontend.

### **Infraestrutura e Deploy**
* **Docker:** Toda a aplicação (e suas dependências) é conteinerizada, garantindo que o sistema rode de forma idêntica em qualquer máquina de desenvolvimento ou ambiente de produção.

---

## 🚀 Como Executar o Projeto Localmente

Graças ao uso do Docker, subir a aplicação é um processo simples e direto.

### Pré-requisitos
* [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/) instalados na sua máquina.
* *Opcional (para rodar fora do contêiner):* Node.js, Java 17+ e Maven.

### Passos para Instalação (Via Docker)

1. **Clone o repositório:**
   ```bash
   git clone [https://github.com/lucachak/FinGen.git](https://github.com/lucachak/FinGen.git)
   cd FinGen