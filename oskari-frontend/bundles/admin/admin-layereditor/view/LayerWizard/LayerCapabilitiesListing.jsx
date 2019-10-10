import React from 'react';
import PropTypes from 'prop-types';
import {List, ListItem} from '../../components/List';

export class LayerCapabilitiesListing extends React.Component {
    getItem (item) {
        return (
            <ListItem onClick={() => this.props.onSelect(item)}>
                {item.name}
            </ListItem>
        );
    }
    render () {
        return (
            <div>
                <List dataSource={this.props.capabilities} renderItem={item => this.getItem(item)}></List>
            </div>
        );
    }
}

LayerCapabilitiesListing.propTypes = {
    capabilities: PropTypes.arrayOf(PropTypes.object),
    onSelect: PropTypes.func
};
