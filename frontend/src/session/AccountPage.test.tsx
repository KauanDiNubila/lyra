import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App.tsx'
import { emptyDay, jsonResponse } from '../diary/testSupport.ts'
import { calls, fetchMock, USER } from './testSupport.ts'

type Server = {
  exportAccount?: () => Promise<Response>
  deleteAccount?: () => Promise<Response>
}

type Download = { href: string; name: string }

let downloads: Download[]
const revokeObjectURL = vi.fn<(url: string) => void>()

function serve({ exportAccount, deleteAccount }: Server = {}) {
  fetchMock.mockImplementation(async (url, init) => {
    const path = String(url)
    if (path === '/api/me/export') {
      return exportAccount ? exportAccount() : new Response('{"version":1}', { headers: { 'Content-Type': 'application/json' } })
    }
    if (path === '/api/me' && init?.method === 'DELETE') {
      return deleteAccount ? deleteAccount() : new Response(null, { status: 204 })
    }
    if (path === '/api/me') return jsonResponse(USER)
    if (path.startsWith('/api/diary')) return jsonResponse(emptyDay(path.split('/')[3]))
    return jsonResponse([])
  })
}

async function openTheAccount() {
  render(<App />)
  fireEvent.click(await screen.findByRole('tab', { name: 'Conta' }))
  await screen.findByRole('heading', { name: 'Conta' })
}

async function askToDelete() {
  fireEvent.click(screen.getByRole('button', { name: 'Apagar minha conta' }))
  const dialog = await screen.findByRole('alertdialog')
  return { dialog, confirm: () => fireEvent.click(within(dialog).getByRole('button', { name: 'Apagar a conta' })) }
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  downloads = []
  revokeObjectURL.mockReset()
  Object.defineProperty(URL, 'createObjectURL', { value: () => 'blob:export', configurable: true, writable: true })
  Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true, writable: true })
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    downloads.push({ href: this.href, name: this.download })
  })
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  Reflect.deleteProperty(URL, 'createObjectURL')
  Reflect.deleteProperty(URL, 'revokeObjectURL')
})

describe('the account tab', () => {
  it('shows who is signed in only when the tab is open', async () => {
    serve()
    render(<App />)
    await screen.findByRole('tab', { name: 'Conta' })
    expect(screen.queryByRole('heading', { name: 'Conta' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: 'Conta' }))

    expect(await screen.findByRole('heading', { name: 'Conta' })).toBeInTheDocument()
    expect(screen.getByText('ana@example.com')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar meus dados' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Apagar minha conta' })).toBeInTheDocument()
  })
})

describe('exporting the data', () => {
  it('downloads the file the server sends and frees the temporary address', async () => {
    serve()
    await openTheAccount()

    fireEvent.click(screen.getByRole('button', { name: 'Exportar meus dados' }))

    await waitFor(() => expect(downloads).toEqual([{ href: 'blob:export', name: 'lyra-export.json' }]))
    expect(calls('/api/me/export')).toHaveLength(1)
    expect(revokeObjectURL).not.toHaveBeenCalled()
    await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:export'), { timeout: 3000 })
  })

  it('says why when the server cannot export and downloads nothing', async () => {
    serve({ exportAccount: () => Promise.resolve(jsonResponse({ detail: 'User not found' }, 404)) })
    await openTheAccount()

    fireEvent.click(screen.getByRole('button', { name: 'Exportar meus dados' }))

    expect(await screen.findByText('Não foi possível exportar')).toBeInTheDocument()
    expect(screen.getByText('User not found')).toBeInTheDocument()
    expect(downloads).toEqual([])
  })

  it('asks the server once when the user clicks twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ exportAccount: () => new Promise((resolve) => { answer = resolve }) })
    await openTheAccount()
    const button = screen.getByRole('button', { name: 'Exportar meus dados' })

    fireEvent.click(button)
    fireEvent.click(button)
    await act(async () => answer(new Response('{}')))

    await waitFor(() => expect(downloads).toHaveLength(1))
    expect(calls('/api/me/export')).toHaveLength(1)
  })
})

describe('deleting the account', () => {
  it('asks for a confirmation that says what will be lost, and does nothing when the user cancels', async () => {
    serve()
    await openTheAccount()

    const { dialog } = await askToDelete()
    expect(within(dialog).getByText('Apagar a conta?')).toBeInTheDocument()
    expect(within(dialog).getByText(/serão apagados para sempre/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    expect(calls('/api/me', 'DELETE')).toHaveLength(0)
    expect(screen.getByRole('heading', { name: 'Conta' })).toBeInTheDocument()
  })

  it('deletes with the CSRF token and goes back to the sign in page', async () => {
    document.cookie = 'XSRF-TOKEN=token-123; path=/'
    serve()
    await openTheAccount()

    const { confirm } = await askToDelete()
    confirm()

    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(calls('/api/me', 'DELETE')).toHaveLength(1)
    expect(new Headers(calls('/api/me', 'DELETE')[0][1]?.headers).get('X-XSRF-TOKEN')).toBe('token-123')
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    expect(screen.queryByText('ana@example.com')).not.toBeInTheDocument()
  })

  it('deletes once when the user confirms twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ deleteAccount: () => new Promise((resolve) => { answer = resolve }) })
    await openTheAccount()
    const { dialog } = await askToDelete()
    const action = within(dialog).getByRole('button', { name: 'Apagar a conta' })

    fireEvent.click(action)
    fireEvent.click(action)

    expect(calls('/api/me', 'DELETE')).toHaveLength(1)
    await act(async () => answer(new Response(null, { status: 204 })))
    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument()
    expect(calls('/api/me', 'DELETE')).toHaveLength(1)
  })

  it('keeps the account and says why when the server cannot delete it', async () => {
    serve({ deleteAccount: () => Promise.resolve(jsonResponse({ detail: 'Invalid CSRF token' }, 403)) })
    await openTheAccount()

    const { confirm } = await askToDelete()
    confirm()

    expect(await screen.findByText('Não foi possível apagar a conta')).toBeInTheDocument()
    expect(screen.getByText('Invalid CSRF token')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    expect(screen.getByRole('tab', { name: 'Conta' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Entrar com Google' })).not.toBeInTheDocument()
  })
})
