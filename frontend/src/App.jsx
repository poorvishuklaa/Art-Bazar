import React from 'react'
import './index.css'
import Navbar from './components/Navbar'
import { Route, Routes } from 'react-router-dom'
import Home from './pages/Home'
import Footer from './components/Footer'

const App = () => {
  return (
    <>
    <div className='app'>
      <Navbar/>
      <Routes>

      <Route path='/' element={<Home />} />


      </Routes>
    </div>
    <Footer />
    </>
  )
}

export default App
