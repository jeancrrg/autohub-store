Feature: API Gateway

  Como ponto de entrada unico do AutoHubStore, o Gateway roteia requisicoes, valida JWT via
  cookie httpOnly, aplica rate limiting e configura CORS, conforme os Criterios de Aceite de
  docs/planning/specs/01-api-gateway.md.

  Scenario: Roteamento com sucesso
    Given que o user-service esta saudavel e retorna o usuario "11111111-1111-1111-1111-111111111111"
    And que eu possuo um cookie de acesso valido
    When eu chamo "GET /api/v1/users/11111111-1111-1111-1111-111111111111" com o cookie de acesso
    Then a resposta possui status 200

  Scenario: Endpoint protegido sem token
    Given que eu nao possuo cookie de acesso
    When eu chamo "GET /api/v1/users/11111111-1111-1111-1111-111111111111" sem o cookie de acesso
    Then a resposta possui status 401

  Scenario Outline: Token expirado ou invalido chega a endpoint protegido
    Given que eu possuo um cookie de acesso "<situacao>"
    When eu chamo "GET /api/v1/users/11111111-1111-1111-1111-111111111111" com o cookie de acesso
    Then a resposta possui status 401

    Examples:
      | situacao                |
      | expirado                 |
      | com assinatura invalida |

  Scenario: Rate limit excedido
    Given que eu ja realizei 100 requisicoes ao endpoint publico "/api/v1/catalog/products/rate-limit-scenario" pelo mesmo cliente na janela atual
    When eu realizo mais uma requisicao a "/api/v1/catalog/products/rate-limit-scenario"
    Then a resposta possui status 429

  Scenario: CORS de origem nao permitida
    Given que a origem "https://origem-nao-permitida.com" nao esta na lista de origens permitidas
    When eu envio uma requisicao de preflight CORS para "/api/v1/catalog/products" com essa origem
    Then a resposta nao contem o header Access-Control-Allow-Origin correspondente
