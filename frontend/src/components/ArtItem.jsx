import React from 'react'
import { assets } from '../assets/assets'



const ArtItem = ({id,name,price,description,image}) => {

  return (
    <div className='art-item'>

        <div className="art-item-img-container">
        <img className="art-item-image" src={image} alt="" />
        </div>


        <div className="art-item-info">
            <div className="art-item-name-rating">
                <p>{name}</p>
                <img src={assets.rating_starts} alt=""/>
            </div>
            <p className="art-item-desc">
                {description}
            </p>
            <p className="art-item-price">
                ${price}
            </p>
        </div>


    </div>
  )
}

export default ArtItem