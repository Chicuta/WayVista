# WayVista

A vanilla-friendly minimap and 3D waypoint plugin for Paper 1.21+.

## Features

- **3D Waypoints** — floating text + soft particles visible in-game
- **Offhand Minimap** — real-time map item showing terrain, player direction and waypoint markers
- **Private & Shared waypoints** — share with everyone or keep to yourself
- **Bilingual** — Portuguese (pt_BR) and English (en_US) via `config.yml`

## Installation

1. Place `wayvista-1.0.0.jar` in your server's `plugins/` folder
2. Restart the server
3. Done!

## Commands

| Command | Description |
|---|---|
| `/waypoint set <name>` | Create a private waypoint at your location |
| `/waypoint set <name> on` | Create a shared waypoint |
| `/waypoint remove <name>` | Delete a waypoint |
| `/waypoint list` | List your waypoints |
| `/waypoint share <name> on/off` | Toggle sharing |
| `/waypoint viewshared on/off` | Show/hide other players' shared waypoints |
| `/minimap on` | Enable minimap (gives map to offhand) |
| `/minimap off` | Disable minimap (removes map) |
| `/minimap toggle` | Toggle on/off |
| `/minimap status` | Check current state |

**Alias:** `/wp` = `/waypoint`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `wayvista.use` | Everyone | Use all commands |
| `wayvista.admin` | OP | Admin controls |

## Configuration (`config.yml`)

```yaml
language: pt_BR          # pt_BR or en_US

limits:
  max-waypoints-per-player: 20

render:
  refresh-ticks: 20       # 3D marker update interval
  particle-distance: 128.0
  text-height: 2.4

minimap:
  scale: 1                # 1 = 1 block/pixel, 2 = wider view
```

---

# WayVista (PT-BR)

Plugin de minimapa e waypoints 3D para Paper 1.21+, compatível com cliente vanilla.

## Funcionalidades

- **Waypoints 3D** — texto flutuante + partículas suaves visíveis no jogo
- **Minimapa na offhand** — mapa em tempo real mostrando terreno, direção e marcadores de waypoints
- **Waypoints privados e compartilhados** — compartilhe com todos ou mantenha só para você
- **Bilíngue** — Português e Inglês configurável no `config.yml`

## Instalação

1. Coloque o `wayvista-1.0.0.jar` na pasta `plugins/` do servidor
2. Reinicie o servidor
3. Pronto!

## Comandos

| Comando | Descrição |
|---|---|
| `/waypoint set <nome>` | Cria waypoint privado na sua posição |
| `/waypoint set <nome> on` | Cria waypoint compartilhado |
| `/waypoint remove <nome>` | Remove um waypoint |
| `/waypoint list` | Lista seus waypoints |
| `/waypoint share <nome> on/off` | Alterna compartilhamento |
| `/waypoint viewshared on/off` | Mostra/esconde waypoints compartilhados de outros |
| `/minimap on` | Ativa minimapa (coloca mapa na offhand) |
| `/minimap off` | Desativa minimapa (remove o mapa) |
| `/minimap toggle` | Alterna ligado/desligado |
| `/minimap status` | Verifica estado atual |

**Alias:** `/wp` = `/waypoint`

## Permissões

| Permissão | Padrão | Descrição |
|---|---|---|
| `wayvista.use` | Todos | Usar todos os comandos |
| `wayvista.admin` | OP | Controles de admin |

## Configuração (`config.yml`)

```yaml
language: pt_BR          # pt_BR ou en_US

limits:
  max-waypoints-per-player: 20

render:
  refresh-ticks: 20       # intervalo de atualização dos marcadores 3D
  particle-distance: 128.0
  text-height: 2.4

minimap:
  scale: 1                # 1 = 1 bloco/pixel, 2 = visão mais ampla
```
