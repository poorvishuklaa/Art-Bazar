import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addToCart, fetchCart, removeCartItem, updateCartItem } from '../api/cart'
import { useAuth } from './useAuth'
import { canShop } from '../utils/roles'

const KEY = ['cart']

/** The server-side cart. Only buyers and artists have one; for everyone else it stays empty. */
export function useCart() {
  const { user, isAuthenticated } = useAuth()
  const enabled = isAuthenticated && canShop(user)
  const query = useQuery({ queryKey: KEY, queryFn: fetchCart, enabled })
  return { ...query, cart: query.data, enabled }
}

/** add / update / remove all return the new cart, which replaces the cached one directly. */
export function useCartMutations() {
  const queryClient = useQueryClient()
  const onSuccess = (cart) => queryClient.setQueryData(KEY, cart)
  return {
    add: useMutation({ mutationFn: ({ artworkId, quantity }) => addToCart(artworkId, quantity), onSuccess }),
    update: useMutation({ mutationFn: ({ artworkId, quantity }) => updateCartItem(artworkId, quantity), onSuccess }),
    remove: useMutation({ mutationFn: (artworkId) => removeCartItem(artworkId), onSuccess }),
  }
}
