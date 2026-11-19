import { useQuery } from '@tanstack/react-query'
import { fetchSalesSummary } from '../api/artistSales'

export const useSalesSummary = () => useQuery({ queryKey: ['sales-summary'], queryFn: fetchSalesSummary })
