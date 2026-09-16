# language: pt
Funcionalidade: User Service

  Como responsavel por cadastro, perfil e enderecos de usuarios do AutoHubStore, o User Service
  valida dados de entrada, garante unicidade de e-mail e expoe endpoints internos consumidos pelo
  Auth Service.

  Cenario: Cadastro de usuario com sucesso
    Dado que o cliente informar dados validos de cadastro
    Quando o cliente enviar a requisicao de cadastro de usuario
    Entao o cliente deve receber resposta com status 201
    E o corpo da resposta deve conter o e-mail cadastrado

  Cenario: Cadastro com e-mail ja existente
    Dado que o cliente ja possuir uma conta cadastrada
    Quando o cliente enviar a requisicao de cadastro de usuario com o mesmo e-mail
    Entao o cliente deve receber resposta com status 409

  Esquema do Cenario: Cadastro com campo obrigatorio ausente
    Dado que o cliente informar dados de cadastro sem o campo "<campo>"
    Quando o cliente enviar a requisicao de cadastro de usuario
    Entao o cliente deve receber resposta com status 400

    Exemplos:
      | campo     |
      | email     |
      | full_name |
      | password  |

  Cenario: Atualizacao de perfil de usuario existente
    Dado que exista um usuario cadastrado
    Quando o cliente enviar a requisicao de atualizacao de perfil com um novo nome
    Entao o cliente deve receber resposta com status 200
    E o corpo da resposta deve conter o nome atualizado

  Cenario: Criacao de endereco de entrega
    Dado que exista um usuario cadastrado
    Quando o cliente enviar a requisicao de criacao de endereco para esse usuario
    Entao o cliente deve receber resposta com status 201

  Cenario: Endereco padrao unico ao atualizar outro endereco como padrao
    Dado que exista um usuario cadastrado com um endereco padrao
    Quando o cliente enviar a requisicao de atualizacao de outro endereco marcando-o como padrao
    Entao o cliente deve receber resposta com status 200
    E o endereco anterior nao deve mais estar marcado como padrao

  Cenario: Endereco padrao unico ao criar novo endereco como padrao
    Dado que exista um usuario cadastrado com um endereco padrao
    Quando o cliente enviar a requisicao de criacao de um novo endereco marcando-o como padrao
    Entao o cliente deve receber resposta com status 201
    E o endereco anterior nao deve mais estar marcado como padrao

  Cenario: Endereco nao padrao nao afeta o endereco padrao existente
    Dado que exista um usuario cadastrado com um endereco padrao
    Quando o cliente enviar a requisicao de criacao de um novo endereco sem marca-lo como padrao
    Entao o cliente deve receber resposta com status 201
    E o endereco padrao existente deve continuar marcado como padrao

  Cenario: Primeiro endereco do usuario cadastrado diretamente como padrao
    Dado que exista um usuario cadastrado sem nenhum endereco
    Quando o cliente enviar a requisicao de criacao do primeiro endereco marcando-o como padrao
    Entao o cliente deve receber resposta com status 201
    E o endereco criado deve estar marcado como padrao

  Cenario: Marcar como padrao um endereco que ja e padrao e idempotente
    Dado que exista um usuario cadastrado com um endereco padrao
    Quando o cliente enviar novamente a requisicao de atualizacao do endereco padrao marcando-o como padrao
    Entao o cliente deve receber resposta com status 200
    E o endereco padrao deve continuar marcado como padrao apos consultar novamente
    E o endereco que nao e padrao nao deve ser afetado

  Cenario: Remocao de endereco existente
    Dado que exista um usuario cadastrado com um endereco cadastrado
    Quando o cliente enviar a requisicao de remocao desse endereco
    Entao o cliente deve receber resposta com status 204

  Cenario: Validacao interna de credenciais correta
    Dado que exista um usuario cadastrado com senha conhecida
    Quando o auth-service enviar a requisicao interna de verificacao de credenciais com a senha correta
    Entao o cliente deve receber resposta com status 200

  Cenario: Validacao interna de credenciais incorreta
    Dado que exista um usuario cadastrado com senha conhecida
    Quando o auth-service enviar a requisicao interna de verificacao de credenciais com a senha incorreta
    Entao o cliente deve receber resposta com status 401

  Cenario: Atualizacao interna de senha
    Dado que exista um usuario cadastrado
    Quando o auth-service enviar a requisicao interna de atualizacao de senha para esse usuario
    Entao o cliente deve receber resposta com status 204
