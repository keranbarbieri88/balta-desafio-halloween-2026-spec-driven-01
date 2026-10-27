# Encurtador de links URL

## Problema
Links longos quebram em mensagens, e-mails e materiais impressos, e não dá para medir quantas pessoas clicaram neles.

## Objetivo
Permitir que uma pessoa transforme uma URL longa em um link curto, compartilhe esse link e acompanhe quantos acessos ele recebeu.

## Usuários
- Criador do link: gera links curtos e acompanha os acessos
- Visitante: clica no link curto e chega ao destino

## Histórias
- Como criador, quero encurtar uma URL para compartilhar um link menor
- Como criador, quero escolher o código do link para usar um nome legível
- Como criador, quero ver quantos acessos meu link recebeu
- Como visitante, quero ser redirecionado ao destino sem passar por tela intermediária via HTTP 302

## Requisitos funcionais
- RF-01: o sistema recebe uma URL de destino e devolve um link curto
- RF-02: o código do link tem 7 caracteres alfanuméricos gerados automaticamente (`[a-zA-Z0-9]`), respeitando diferenciação de maiúsculas e minúsculas (*case-sensitive*)
- RF-03: o criador pode informar um código próprio, entre 4 e 20 caracteres
- RF-04: acessar um link curto existente na raiz (`GET /{code}`) redireciona para a URL de destino com status HTTP 302 (Found)
- RF-05: cada acesso a um link válido incrementa o contador daquele link
- RF-06: o criador pode definir opcionalmente uma data e hora de expiração no formato ISO-8601 em UTC (`YYYY-MM-DDTHH:mm:ssZ`); se não informada, o link não expira
- RF-07: o sistema devolve o total de acessos de um link para qualquer pessoa que conheça e consulte o código do link (consulta pública e sem autenticação)
- RF-08: os links continuam válidos após reinício da aplicação

## Regras de negócio
- RN-01: o código do link é único em todo o sistema e sensível a maiúsculas e minúsculas (*case-sensitive*)
- RN-02: a URL de destino precisa usar HTTP ou HTTPS
- RN-03: link expirado não redireciona e não conta acesso
- RN-04: o contador de acessos nunca diminui
- RN-05: códigos personalizados aceitam apenas caracteres alfanuméricos, hífens e underscores (`^[a-zA-Z0-9_-]{4,20}$`) com validação *case-sensitive*
- RN-06: a URL de destino não pode apontar para o próprio domínio da aplicação (prevenção de loop), validando o host de destino contra os hosts configurados do sistema (incluindo `localhost` e `127.0.0.1`)
- RN-07: termos reservados do sistema (`api`, `actuator`, `swagger`, `swagger-ui`, `v3`, `health`) não podem ser usados como código personalizado e nunca serão gerados automaticamente pelo sistema

## Casos de borda
- Código já em uso: recusar a criação e explicar o motivo com status HTTP 409 (Conflict)
- Código inexistente no acesso: responder status HTTP 404 (Not Found) no formato RFC 7807 (ProblemDetails)
- URL de destino com mais de 2048 caracteres: recusar com status HTTP 400 (Bad Request)
- Data de expiração no passado ou em formato inválido: recusar na criação com status HTTP 400 (Bad Request)
- Tentativa de encurtar URL do próprio encurtador: recusar com erro de validação com status HTTP 400 (Bad Request)
- Código reservado utilizado: recusar com erro de validação com status HTTP 400 (Bad Request)
- Tentativa concorrente com mesmo código: banco deve garantir unicidade via constraint e API retornar conflito com status HTTP 409 (Conflict)
- Link expirado no acesso: responder status HTTP 410 (Gone) no formato RFC 7807 (ProblemDetails) sem redirecionamento

## Fora de escopo
- Cadastro e login dos usuários
- Domínio personalizado
- Relatório com origem geográfica do acesso
- Edição do destino de um link já criado

## Critérios de aceite
- Criar um link e acessá-lo redireciona para o destino informado via HTTP 302
- Dois links não podem ter o mesmo código (respeitando *case sensitivity*)
- Link com data de expiração vencida responde HTTP 410 (Gone) com RFC 7807 e não incrementa o contador
- Um link com expiração definida funciona estritamente até o instante configurado em UTC
- Qualquer pessoa que conheça o código pode consultar o total de acessos
- O gerador automático nunca gera códigos que coincidam com termos reservados
- Após reiniciar a aplicação, os links persistidos no banco continuam funcionando