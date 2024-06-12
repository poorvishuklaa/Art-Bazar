import React, { useState } from 'react'
import Header from '../components/Header'
import ExploreCategory from '../components/ExploreCategory';
import ArtDisplay from '../components/ArtDisplay';


const Home = () => {
  const[category,setCategory]=useState("All");
  return (
    <div>
        <Header />
        <ExploreCategory category={category} setCategory={setCategory}/>
        <ArtDisplay category={category} />
    
      
    </div>
  )
}

export default Home