import React, {useState} from 'react'
import { assets } from '../assets/assets'
const Navbar = () => {
  
  const[menu,setMenu]=useState("menu");



  return (
    <div className='navbar'>
        <img className='logo' src={assets.logo} alt=""/>
        <ul className='navbar-menu'>
            <li onClick={()=>setMenu('home')} className={menu==="home"?"active":""}>Home</li>

            <li onClick={()=>setMenu('categories')} className={menu==="categories"?"active":""}>Categories</li>

            <li onClick={()=>setMenu('about')} className={menu==="about"?"active":""}>About</li>

            <li onClick={()=>setMenu('contact')} className={menu==="contact"?"active":""}>Contact us</li>
        </ul>

        <div className="navbar-right">
            <img src={assets.search_icon} alt="" />
            <div className="navbar-search-icon">
                <img src={assets.bag_icon} alt="" />
                <div className="dot"></div>
            </div>
            <button>Sign In</button>
        </div>
    </div>
  )
}

export default Navbar