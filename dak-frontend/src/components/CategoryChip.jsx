function CategoryChip({ name, active = false, onClick }) {
    return (
      <button
        onClick={onClick}
        className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap ${
          active ? 'bg-korea-blue text-white' : 'bg-white text-ink border border-gray-200'
        }`}
      >
        {name}
      </button>
    );
  }
  
  export default CategoryChip;