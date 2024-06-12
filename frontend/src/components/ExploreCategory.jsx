import React from 'react'
import { menu_list } from '../assets/assets'

const ExploreCategory = ({category,setCategory}) => {
  return (
    <div className='explore-menu' id='explore-menu'>
    <h1>Explore our Category</h1>
    <p className='explore-menu-text'>Discover unique and original artworks at Art Bazar. Our curated collection of paintings and sculptures is designed to elevate your home décor and reflect your personal style..</p>

    <div className='explore-menu-list'>
        {menu_list.map((item,index)=>{
            return(
                <div onClick={()=>setCategory(prev=>prev===item.menu_name?"All":item.menu_name)} key={index} className='explore-menu-list-item'>
                <img className={category===item.menu_name?"active":""}src ={item.menu_image} alt="" />
                <p>{item.menu_name}</p>
                </div>
            )
        })}
    </div>
    <hr />
    </div>
  )
}

export default ExploreCategory