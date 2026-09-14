# language: pt
Funcionalidade: Auth Service

  Como servico de autenticacao do AutoHubStore, o Auth Service realiza login, logout, refresh
  com rotation, blacklist de tokens revogados no Redis e reset de senha, delegando toda
  validacao de credencial ao User Service via OpenFeign.

  Cenario: Login com sucesso
    Dado que o user-service estar saudavel e aceitar a credencial do cliente
    Quando o cliente chamar o endpoint de login com e-mail e senha corretos
    Entao o cliente deve receber resposta com status 200
    E a resposta deve conter cookies de acesso e de refresh httpOnly

  Cenario: Login com credencial invalida
    Dado que o user-service estar saudavel e rejeitar a credencial do cliente
    Quando o cliente chamar o endpoint de login com e-mail e senha corretos
    Entao o cliente deve receber resposta com status 401

  Cenario: Refresh com rotation
    Dado que o cliente possuir um refresh token valido emitido em um login anterior
    Quando o cliente chamar o endpoint de refresh com esse refresh token
    Entao o cliente deve receber resposta com status 200
    Quando o cliente chamar o endpoint de refresh novamente com o refresh token anterior
    Entao o cliente deve receber resposta com status 401

  Cenario: Logout revoga sessao
    Dado que o cliente estar autenticado com access token e refresh token validos
    Quando o cliente chamar o endpoint de logout
    Entao o cliente deve receber resposta com status 200
    E o access token utilizado deve estar registrado na blacklist do Redis
    Quando o cliente chamar o endpoint de refresh com o refresh token revogado pelo logout
    Entao o cliente deve receber resposta com status 401

  Cenario: Reset de senha completo
    Dado que o cliente possuir um token de redefinicao de senha valido
    E que o user-service aceitar a atualizacao de senha do usuario do token
    Quando o cliente chamar o endpoint de redefinicao de senha com esse token e uma nova senha
    Entao o cliente deve receber resposta com status 204

  Cenario: Circuit breaker aberto no User Service
    Dado que o user-service estar indisponivel de forma consecutiva
    Quando o cliente chamar o endpoint de login repetidamente ate abrir o circuit breaker
    Entao o cliente deve receber resposta com status 503
