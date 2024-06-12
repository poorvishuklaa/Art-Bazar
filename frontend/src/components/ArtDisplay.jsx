import React from 'react'
import { useContext } from 'react'
import { StoreContext } from '../context/StoreContext'
import ArtItem from './ArtItem'

const ArtDisplay = ({category}) => {

    const {Art_list}=useContext(StoreContext)


  return (
    <div className='art-display' id='food-display'>

    <h2>Art Works near you</h2>

    <div className="art-display-list">

    {Art_list.map((item,index)=>{
              if(category==="All" || category===item.category)
              {
                return <ArtItem 
                key={index} 
                id={item._id} 
                name={item.name} description={item.description} 
                price={item.price} 
                image={item.image}/>
              }
            })} 
    </div>

    </div>
  )
}

export default ArtDisplay