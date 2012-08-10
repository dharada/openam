/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 16:51
 * To change this template use File | Settings | File Templates.
 */

Ext.define('OpenAM.controller.Global', {
    extend: 'Ext.app.controller',

    views: []
        'serviceList',
        'serviceEditor'
    ],

    init: function() {
        this.control({
            'servicelist': {
                itemdblclick: this.editService
            }
        });
        console.log('initialized Global Controller');
    },
    editService: function(grid,record)) {
        console.log('DoubleClick on ' + record.get('name')) ;
    }
})