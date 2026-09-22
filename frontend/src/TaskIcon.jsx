import { CookingPot, Sofa, Bath, Package, Recycle } from 'lucide-react';
const icons = [CookingPot, Sofa, Bath, Package, Recycle];
import { createElement } from 'react';
export default function TaskIcon({ id }) { return createElement(icons[id] || Recycle, {size:24, 'aria-hidden':true}); }
