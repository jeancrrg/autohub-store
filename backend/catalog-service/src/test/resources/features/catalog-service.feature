# language: pt
Funcionalidade: Catalog Service

  Como responsavel pelo catalogo de produtos do AutoHubStore, o Catalog Service gerencia produtos,
  categorias e marcas com cache Redis, upload de imagens e publicacao de eventos Kafka.

  Cenario: Listagem com cache
    Dado que um produto ja tenha sido criado no catalogo
    E que o produto ja tenha sido consultado uma vez, populando o cache
    Quando o preco do produto for alterado diretamente no banco, contornando o servico
    E o cliente consultar o produto novamente
    Entao o cliente deve receber resposta com status 200
    E a resposta deve conter o preco anterior, obtido do cache

  Cenario: Criacao de produto com sucesso
    Dado que uma categoria e uma marca existentes estejam cadastradas
    Quando o administrador enviar a requisicao de criacao de produto com dados validos
    Entao o cliente deve receber resposta com status 201
    E o evento de produto criado deve ser publicado no topico correspondente

  Cenario: Criacao com SKU duplicado
    Dado que exista um produto cadastrado com sku conhecido
    Quando o administrador enviar a requisicao de criacao de produto reusando esse sku
    Entao o cliente deve receber resposta com status 409

  Cenario: Criacao de produto com marca existente
    Dado que uma categoria e uma marca existentes estejam cadastradas
    Quando o administrador enviar a requisicao de criacao de produto com dados validos
    Entao o cliente deve receber resposta com status 201
    E a resposta deve conter a marca resolvida com nome e slug

  Cenario: Criacao de produto com marca inexistente
    Dado que uma categoria e uma marca existentes estejam cadastradas
    Quando o administrador enviar a requisicao de criacao de produto informando um brandId inexistente
    Entao o cliente deve receber resposta com status 404

  Cenario: Upload de imagem valida
    Dado que um produto ja tenha sido criado no catalogo
    Quando o admin enviar upload de uma imagem PNG valida
    Entao o cliente deve receber resposta com status 201
    E a primeira imagem cadastrada deve estar marcada como principal

  Esquema do Cenario: Upload de imagem invalida
    Dado que um produto ja tenha sido criado no catalogo
    Quando o admin enviar upload de arquivo "<nome_arquivo>" do tipo "<tipo_conteudo>" com <tamanho_bytes> bytes
    Entao o cliente deve receber resposta com status <status>

    Exemplos:
      | nome_arquivo     | tipo_conteudo   | tamanho_bytes | status |
      | documento.pdf     | application/pdf | 1024           | 415    |
      | foto-grande.png   | image/png       | 6291456        | 413    |

  Cenario: Atualizacao invalida cache
    Dado que um produto ja tenha sido criado no catalogo
    E que o produto ja tenha sido consultado uma vez, populando o cache
    Quando o administrador enviar a requisicao de atualizacao alterando o preco do produto
    Entao o cliente deve receber resposta com status 200
    E a consulta ao produto deve refletir o preco atualizado
    E o evento de produto atualizado deve ser publicado no topico correspondente

  Cenario: Listagem de marcas
    Quando o cliente consultar a lista de marcas
    Entao o cliente deve receber resposta com status 200
    E a lista de marcas deve estar ordenada alfabeticamente por nome

  Cenario: Atualizacao de marca do produto
    Dado que um produto ja tenha sido criado no catalogo
    Quando o administrador enviar a requisicao de atualizacao alterando a marca do produto
    Entao o cliente deve receber resposta com status 200
    E a consulta ao produto deve refletir a marca atualizada

  Cenario: Resposta de produto sem estoque
    Dado que um produto ja tenha sido criado no catalogo
    Quando o cliente consultar o produto criado
    Entao o corpo da resposta do produto nao deve conter o campo de quantidade em estoque

  Cenario: Criacao de categoria em lista plana
    Quando o administrador enviar a requisicao de criacao de categoria com nome e slug unicos
    Entao o cliente deve receber resposta com status 201
