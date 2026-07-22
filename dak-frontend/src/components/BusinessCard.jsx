function BusinessCard({ business }) {
    return (
      <div className="bg-white rounded-2xl shadow-sm p-4 border border-gray-100">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-ink">{business.name}</h3>
          {business.verified && (
            <span className="text-xs bg-korea-blue text-white px-2 py-0.5 rounded-full">
              인증됨
            </span>
          )}
        </div>
        <p className="text-sm text-gray-500 mt-1">{business.suburb}</p>
      </div>
    );
  }
  
  export default BusinessCard;