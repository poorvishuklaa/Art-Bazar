import { Route, Routes } from 'react-router-dom'
import Layout from './components/layout/Layout'
import Home from './pages/Home'
import ComingSoon from './pages/ComingSoon'
import NotFound from './pages/NotFound'

const App = () => (
  <Routes>
    <Route element={<Layout />}>
      <Route index element={<Home />} />
      <Route path="login" element={<ComingSoon title="Sign in" />} />
      <Route path="register" element={<ComingSoon title="Create an account" />} />
      <Route path="cart" element={<ComingSoon title="Your cart" />} />
      <Route path="*" element={<NotFound />} />
    </Route>
  </Routes>
)

export default App
