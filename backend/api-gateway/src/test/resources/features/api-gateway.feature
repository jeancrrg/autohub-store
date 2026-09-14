# language: pt
Funcionalidade: API Gateway

  Como ponto de entrada unico do AutoHubStore, o Gateway roteia requisicoes, valida JWT via
  cookie httpOnly, aplica rate limiting e configura CORS.

  Cenario: Roteamento com sucesso
    Dado que o user-service estar saudavel e retornar o usuario "11111111-1111-1111-1111-111111111111"
    E que o cliente possuir um cookie de acesso valido
    Quando o cliente chamar o endpoint de usuarios com o cookie de acesso
    Entao o cliente deve receber resposta com status 200

  Cenario: Endpoint protegido sem token
    Dado que o cliente nao possuir cookie de acesso
    Quando o cliente chamar o endpoint de usuarios sem o cookie de acesso
    Entao o cliente deve receber resposta com status 401

  Esquema do Cenario: Token expirado ou invalido chega a endpoint protegido
    Dado que o cliente possuir um cookie de acesso "<situacao>"
    Quando o cliente chamar o endpoint de usuarios com o cookie de acesso
    Entao o cliente deve receber resposta com status 401

    Exemplos:
      | situacao                |
      | expirado                |
      | com assinatura invalida |

  Cenario: Acesso com token na blacklist
    Dado que o cliente possuir um cookie de acesso valido com um jti conhecido
    E que esse jti estar na blacklist de tokens revogados
    Quando o cliente chamar o endpoint de usuarios com o cookie de acesso
    Entao o cliente deve receber resposta com status 401

  Cenario: Rate limit excedido
    Dado que o cliente ja ter realizado 100 requisicoes ao endpoint publico do catalogo na janela atual
    Quando o cliente realizar mais uma requisicao a esse endpoint
    Entao o cliente deve receber resposta com status 429

  Cenario: CORS de origem nao permitida
    Dado que a origem "https://origem-nao-permitida.com" nao estar na lista de origens permitidas
    Quando o gateway receber uma requisicao de preflight CORS do catalogo com essa origem
    Entao a resposta nao deve conter o header Access-Control-Allow-Origin correspondente
