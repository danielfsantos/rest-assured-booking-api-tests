# Rest Assured Booking API Tests

Projeto de **testes automatizados de API** em **Java**, utilizando **RestAssured**, **JUnit 5** e **Allure Report**, cobrindo os principais endpoints da API pública [restful-booker](https://restful-booker.herokuapp.com/), usada como ambiente de prática para testes de reservas (bookings).

## 🛠️ Tecnologias

- [Java 17](https://openjdk.org/)
- [Maven](https://maven.apache.org/) — gerenciador de dependências e build
- [RestAssured](https://rest-assured.io/) — biblioteca para testes de APIs REST
- [JUnit 5](https://junit.org/junit5/) — framework de testes
- [Gson](https://github.com/google/gson) — serialização/deserialização JSON
- [Allure Report](https://allurereport.org/) — relatórios de execução
- [Groovy](https://groovy-lang.org/) — usado nas specs de request/response

## 📁 Estrutura do projeto

```
.
├── src/test/java/
│   ├── apitests/
│   │   └── BookingApiTest.java          # Casos de teste da API de bookings
│   ├── base/
│   │   └── BaseTest.java                # Configuração base (baseURI)
│   └── com/assureapi/
│       ├── client/
│       │   ├── AuthClient.java          # (reservado para autenticação futura)
│       │   └── UserClient.java          # (reservado para operações de usuário futuras)
│       ├── model/
│       │   ├── Booking.java             # POJO de uma reserva
│       │   └── BookingDates.java        # POJO das datas de check-in/checkout
│       ├── specs/
│       │   └── InitialSpecs.groovy      # Specs de request/response (RestAssured)
│       └── utils/
│           └── ConfigReader.java        # Leitura de configurações (config.properties)
├── src/test/resources/
│   ├── allure.properties                # Diretório de resultados do Allure
│   └── config.properties                # URL base da API sob teste
├── .github/workflows/
│   └── api-tests.yml                    # Pipeline de CI (GitHub Actions)
└── pom.xml
```

## ✅ Pré-requisitos

- JDK 17+
- [Maven](https://maven.apache.org/install.html) 3.8+

## 🚀 Instalação

```bash
git clone https://github.com/danielfsantos/rest-assured-booking-api-tests.git
cd rest-assured-booking-api-tests
mvn clean install -DskipTests
```

## ▶️ Executando os testes

Rodar toda a suíte:

```bash
mvn clean test
```

Gerar e visualizar o relatório do Allure:

```bash
mvn allure:report
mvn allure:serve
```

A URL base da API é definida em `src/test/resources/config.properties` (`base.uri`) e pode ser sobrescrita via propriedade do sistema:

```bash
mvn clean test -Dbase.uri=https://outra-api.exemplo.com
```

## 🧪 Casos de teste cobertos

| Teste | Descrição |
|---|---|
| `testListingBooks` | Lista todas as reservas e valida o status 200 |
| `testGetBookingParam` | Busca uma reserva por ID e valida os campos retornados |
| `testCreateNewBooking` | Cria uma nova reserva e valida os dados retornados |
| `testBookingNotFound` | Busca uma reserva inexistente e valida o status 404 |

## 🔄 Integração Contínua (CI)

O workflow do **GitHub Actions** (`.github/workflows/api-tests.yml`) roda a cada `push`/`pull request` na branch `main`:

1. Faz checkout do código
2. Configura o JDK 17 (Temurin)
3. Executa os testes com `mvn clean test`
4. Gera o relatório do Allure (`mvn allure:report`)
5. Publica o relatório como artefato do workflow (`allure-report`)

## 🗺️ Próximos passos

- Implementar `AuthClient` para os fluxos de autenticação (`/auth`)
- Implementar `UserClient` para operações de atualização/remoção de reservas
- Utilizar as specs de `InitialSpecs.groovy` diretamente nos testes (request/response specification)

## 👤 Autor

**Daniel Santos**
[github.com/danielfsantos](https://github.com/danielfsantos)

## 📄 Licença

Este projeto não possui uma licença definida. Caso deseje reutilizá-lo, entre em contato com o autor.
