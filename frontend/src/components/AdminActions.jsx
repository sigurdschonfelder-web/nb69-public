const API = import.meta.env.VITE_API_BASE ?? "";

export default function AdminActions({ isLoggedIn, navn, refresh }) {

    const url = `${API}/api/prikker/update`;

    const updatedData = (b) => ({
        navn: navn,
        increase: b,
    });

    const handleClick = async (k) => {
        const data = await fetch(url, 
            {method: "PUT", 
            body: JSON.stringify(updatedData(k)), 
            headers: { Accept: "application/json", "Content-Type": "application/json", }}
        )
        console.log(data);
        const text = await data.text();
        console.log("raw response: ", text);
        await refresh();
    }


    return (
        <>
            <th>{isLoggedIn && (<><button onClick={() => handleClick(false)}>-</button></>)}</th>
            <th>{isLoggedIn && (<><button onClick={() => handleClick(true)}>+</button></>)}</th>
        </>
    );

}