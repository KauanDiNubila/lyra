import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App.tsx'
import { emptyDay, jsonResponse } from '../diary/testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()
const USER = { id: 'user-1', email: 'ana@example.com', name: 'Ana Souza' }

type Server = {
  me?: () => Promise<Response>
  logout?: () => Promise<Response>
}

function unauthorized(): Response {
  return jsonResponse({ status: 401, title: 'Unauthorized', detail: 'Sign in to use the API' }, 401)
}

function serve({ me, logout }: Server = {}) {
  fetchMock.mockImplementation(async (url, init) => {
    const path = String(url)
    if (path === '/api/me') return me ? me() : jsonResponse(USER)
    if (path === '/api/logout') return logout ? logout() : new Response(null, { status: 204 })
    if (path.startsWith('/api/diary')) {
      return init?.method === 'POST'
        ? jsonResponse({ detail: 'There is no active plan' }, 404)
        : jsonResponse(emptyDay(path.split('/')[3]))
    }
    return jsonResponse([])
  })
}

function calls(path: string) {
  return fetchMock.mock.calls.filter(([url]) => String(url) === path)
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  window.history.replaceState(null, '', '/')
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

describe('opening the app', () => {
  it('shows a spinner while it asks who is signed in', async () => {
    serve({ me: () => new Promise(() => {}) })
    render(<App />)

    expect(await screen.findByLabelText('Carregando a sessão')).toBeInTheDocument()
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Entrar com Google' })).not.toBeInTheDocument()
  })

  it('shows the app with the name of the user when there is a session', async () => {
    serve()
    render(<App />)

    expect(await screen.findByText('Ana Souza')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Diário' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Entrar com Google' })).not.toBeInTheDocument()
  })

  it('shows the sign in page, and no tab, when there is no session', async () => {
    serve({ me: () => Promise.resolve(unauthorized()) })
    render(<App />)

    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.map(([url]) => String(url))).toEqual(['/api/me'])
  })

  it('explains a failure to reach the server and tries again', async () => {
    serve()
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    render(<App />)

    expect(await screen.findByText('Não foi possível abrir o app')).toBeInTheDocument()
    expect(screen.getByText('Sem conexão com o servidor. Verifique a internet e tente de novo.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('Ana Souza')).toBeInTheDocument()
  })
})

describe('the sign in page', () => {
  it('sends the user to Google with a real link', async () => {
    serve({ me: () => Promise.resolve(unauthorized()) })
    render(<App />)

    const link = await screen.findByRole('link', { name: 'Entrar com Google' })

    expect(link).toHaveAttribute('href', '/oauth2/authorization/google')
  })

  it('does not show a warning on a normal visit', async () => {
    serve({ me: () => Promise.resolve(unauthorized()) })
    render(<App />)

    await screen.findByRole('link', { name: 'Entrar com Google' })

    expect(screen.queryByText('Não foi possível entrar')).not.toBeInTheDocument()
  })

  it('warns when Google sent the user back with a failure and cleans the address', async () => {
    window.history.replaceState(null, '', '/?login=failed')
    serve({ me: () => Promise.resolve(unauthorized()) })
    render(<App />)

    expect(await screen.findByText('Não foi possível entrar')).toBeInTheDocument()
    expect(screen.getByText('Use uma conta Google com o e-mail verificado e tente de novo.')).toBeInTheDocument()
    await waitFor(() => expect(window.location.search).toBe(''))
  })
})

describe('signing out', () => {
  it('asks the server, with the CSRF token, and goes back to the sign in page', async () => {
    document.cookie = 'XSRF-TOKEN=token-123; path=/'
    serve()
    render(<App />)
    await screen.findByText('Ana Souza')

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(calls('/api/logout')).toHaveLength(1)
    expect(calls('/api/logout')[0][1]?.method).toBe('POST')
    expect(new Headers(calls('/api/logout')[0][1]?.headers).get('X-XSRF-TOKEN')).toBe('token-123')
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    expect(screen.queryByText('Ana Souza')).not.toBeInTheDocument()
  })

  it('signs out once when the user clicks twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ logout: () => new Promise((resolve) => { answer = resolve }) })
    render(<App />)
    await screen.findByText('Ana Souza')
    const button = screen.getByRole('button', { name: 'Sair' })

    fireEvent.click(button)
    fireEvent.click(button)

    expect(calls('/api/logout')).toHaveLength(1)
    await act(async () => answer(new Response(null, { status: 204 })))
    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(calls('/api/logout')).toHaveLength(1)
  })

  it('stays in the app and says why when the server cannot sign the user out', async () => {
    serve({ logout: () => Promise.resolve(jsonResponse({ detail: 'Invalid CSRF token' }, 403)) })
    render(<App />)
    await screen.findByText('Ana Souza')

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByText('Não foi possível sair')).toBeInTheDocument()
    expect(screen.getByText('Invalid CSRF token')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Diário' })).toBeInTheDocument()
  })
})

describe('a session that ends while the user is in the app', () => {
  it('sends the user back to the sign in page when any call answers 401', async () => {
    let signedIn = true
    fetchMock.mockImplementation(async (url) => {
      const path = String(url)
      if (path === '/api/me') return jsonResponse(USER)
      if (!signedIn) return unauthorized()
      if (path.startsWith('/api/diary')) return jsonResponse(emptyDay(path.split('/')[3]))
      return jsonResponse([])
    })
    render(<App />)
    await screen.findByText('Ana Souza')
    signedIn = false

    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))

    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
  })

  it('does not keep the screens of the last user after the session ended', async () => {
    serve()
    render(<App />)
    await screen.findByText('Ana Souza')
    expect(within(screen.getByRole('tabpanel', { hidden: false })).getByRole('heading', { name: 'Diário' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }))

    await screen.findByRole('link', { name: 'Entrar com Google' })
    expect(screen.queryByRole('heading', { name: 'Diário' })).not.toBeInTheDocument()
  })
})
