
import React from 'react';
import PropTypes from 'prop-types';
import AntBadge from 'antd/lib/badge';
import 'antd/lib/badge/style/css';

export const Badge = ({count, inversed}) => {
    const style = {
        backgroundColor: inversed ? '#333' : '#999',
        color: '#fff',
        fontWeight: '700',
        whiteSpace: 'nowrap',
        textShadow: '0 -1px 0 rgba(0,0,0,.25)'
    };
    return <AntBadge count={count} style={style} />;
};
Badge.propTypes = {
    count: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
    ]),
    inversed: PropTypes.bool
};
